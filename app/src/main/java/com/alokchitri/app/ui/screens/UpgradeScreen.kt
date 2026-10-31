package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.InfoBanner
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes

/**
 * Prices come from Google Play (ProductDetails.formattedPrice), never hard-coded: the design shows
 * about ৳199, early-user ৳149. [price] / [strikePrice] are the formatted strings; strikePrice may be null.
 */
data class UpgradeOffer(val price: String, val strikePrice: String?, val topUpLabel: String)

/** Screen 11 · Lifetime unlock. Opened from Home (settings), Feedback (out of quota) and Recipes. */
@Composable
fun UpgradeScreen(
    offer: UpgradeOffer,
    onClose: () -> Unit,
    onBuy: () -> Unit,
    onBuyTopUp: () -> Unit,
    onGiftCardGuide: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp)) {
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Close, stringResource(R.string.cd_close), tint = AlokColors.Text)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.upgrade_title), style = MaterialTheme.typography.displayMedium, color = AlokColors.Text)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.upgrade_sub), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
            Spacer(Modifier.height(16.dp))

            AlokCard(Modifier.fillMaxWidth(), borderColor = AlokColors.Amber, shape = AlokShapes.Hero, padding = 20.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Text(offer.price, style = MaterialTheme.typography.displayMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit(40f, androidx.compose.ui.unit.TextUnitType.Sp)), color = AlokColors.Amber)
                    if (offer.strikePrice != null) {
                        Text(offer.strikePrice, style = MaterialTheme.typography.titleLarge, color = AlokColors.TextMuted, textDecoration = TextDecoration.LineThrough)
                        Box(Modifier.clip(CircleShape).background(AlokColors.AmberTint).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text(stringResource(R.string.upgrade_early), style = MaterialTheme.typography.labelSmall, color = AlokColors.Amber)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                listOf(R.string.upgrade_b1, R.string.upgrade_b2, R.string.upgrade_b3, R.string.upgrade_b4).forEach { res ->
                    Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Check, null, tint = AlokColors.Green, modifier = Modifier.size(22.dp))
                        Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = AlokColors.Text)
                    }
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton(stringResource(R.string.upgrade_cta_fmt, offer.price), onBuy)
                Text(
                    stringResource(R.string.upgrade_play),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AlokColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))
            AlokCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.topup_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = AlokColors.Text)
                        Text(stringResource(R.string.topup_sub), style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                    }
                    Box(
                        Modifier.height(48.dp).clip(CircleShape).border(1.dp, AlokColors.Outline, CircleShape).clickable(onClick = onBuyTopUp).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(offer.topUpLabel, style = MaterialTheme.typography.labelMedium, color = AlokColors.Text) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().clickable(onClick = onGiftCardGuide)) {
            Column {
                InfoBanner(Icons.Outlined.CreditCard, AlokColors.Green, stringResource(R.string.giftcard_title) + " " + stringResource(R.string.giftcard_sub))
            }
        }
    }
}
