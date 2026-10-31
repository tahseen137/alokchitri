// POST /api/photo-feedback: the app's Photo feedback screen (screen 9) sends one downscaled JPEG plus the
// settings read from its EXIF; Claude answers with "what happened" and "next time" in the user's language.
import Anthropic from "@anthropic-ai/sdk";
import { betaZodOutputFormat } from "@anthropic-ai/sdk/helpers/beta/zod";
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";

const client = new Anthropic(); // ANTHROPIC_API_KEY from the Vercel project's environment variables
const MODEL = process.env.FEEDBACK_MODEL ?? "claude-opus-5-5";

// The app sends at most 1280 px on the long side at JPEG quality 80, well under 1 MB of base64.
const MAX_IMAGE_BASE64 = 1_500_000;

const RequestSchema = z.object({
  image: z.string().min(100).max(MAX_IMAGE_BASE64),
  language: z.enum(["bn", "en"]),
  goal: z.string().max(40).optional(),
  phone: z.string().max(80).optional(),
  exif: z
    .object({
      iso: z.number().int().positive().optional(),
      shutter: z.string().max(16).optional(),
      aperture: z.number().positive().optional(),
      focalLength: z.number().positive().optional(),
      flash: z.boolean().optional(),
    })
    .default({}),
});

const FeedbackSchema = z.object({
  whatHappened: z.string(),
  nextTime: z.string(),
});

const SYSTEM = `You are Alokchitri's photo coach for beginner phone photographers in Bangladesh.
You look at one photo and the camera settings it was taken with, and give practical feedback.

Write two short parts:
- whatHappened: 1-2 sentences on the most important problem or strength you see (noise, blur, exposure, white balance, focus, composition), tied to the settings when they explain it.
- nextTime: 1-2 sentences with concrete settings or actions for the same scene on a phone's Pro mode (ISO, shutter like 1/60, white balance in K, EV, holding the phone steady). Only suggest what a phone can do.

Use plain everyday words, no jargon beyond setting names. If settings are missing, judge from the picture alone and do not mention that they are missing. Write in the language you are asked for; in Bangla keep setting names and numbers in English (ISO 800, 1/60, 4200K).`;

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "POST") return res.status(405).json({ error: "method_not_allowed" });

  const parsed = RequestSchema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: "bad_request" });
  const { image, language, goal, phone, exif } = parsed.data;

  const settings = [
    exif.iso && `ISO ${exif.iso}`,
    exif.shutter && `shutter ${exif.shutter}`,
    exif.aperture && `f/${exif.aperture}`,
    exif.focalLength && `${exif.focalLength} mm`,
    exif.flash !== undefined && (exif.flash ? "flash fired" : "no flash"),
  ].filter(Boolean);

  const context = [
    `Answer in ${language === "bn" ? "Bangla" : "English"}.`,
    goal && `The user was shooting: ${goal}.`,
    phone && `Phone: ${phone}.`,
    settings.length ? `Settings: ${settings.join(", ")}.` : "Settings: unknown.",
  ].filter(Boolean).join("\n");

  try {
    const response = await client.beta.messages.parse({
      model: MODEL,
      max_tokens: 2000,
      // A short, single-photo critique: low effort keeps it fast and cheap.
      output_config: { effort: "low", format: betaZodOutputFormat(FeedbackSchema) },
      betas: ["server-side-fallback-2026-07-01"],
      fallbacks: "default", // a policy decline is retried on Anthropic's recommended model instead of failing
      system: SYSTEM,
      messages: [
        {
          role: "user",
          content: [
            { type: "image", source: { type: "base64", media_type: "image/jpeg", data: image } },
            { type: "text", text: context },
          ],
        },
      ],
    });

    if (response.stop_reason === "refusal" || !response.parsed_output) {
      return res.status(422).json({ error: "no_feedback" });
    }
    return res.status(200).json(response.parsed_output);
  } catch (err) {
    if (err instanceof Anthropic.BadRequestError) return res.status(400).json({ error: "bad_image" });
    if (err instanceof Anthropic.RateLimitError) return res.status(503).json({ error: "busy" });
    console.error("photo-feedback failed", err);
    return res.status(502).json({ error: "upstream" });
  }
}
