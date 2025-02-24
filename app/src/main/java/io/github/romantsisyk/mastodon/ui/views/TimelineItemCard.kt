package io.github.romantsisyk.mastodon.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import io.github.romantsisyk.mastodon.R
import io.github.romantsisyk.mastodon.domain.model.Account
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.AppConstants.DEFAULT_ANIMATION_DURATION
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

@Composable
fun TimelineItemCard(
    item: TimelineItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            isVisible = false
            delay(DEFAULT_ANIMATION_DURATION.toLong())
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = DEFAULT_ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(),
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = { DismissBackground(dismissState) },
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    FormattedHtmlText(
                        htmlText = item.content,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = item.createdAt.formatRelative(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color by animateColorAsState(
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.Settled -> Color.Transparent
            else -> MaterialTheme.colorScheme.errorContainer
        },
        label = "background color"
    )
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val icon = Icons.Default.Delete
    val scale by animateFloatAsState(
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "icon scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = dimensionResource(id = R.dimen.padding_large)),
        contentAlignment = alignment
    ) {
        Icon(
            icon,
            contentDescription = "Delete",
            modifier = Modifier.scale(scale),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun FormattedHtmlText(
    htmlText: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(htmlText) {
        buildAnnotatedString {
            append(
                HtmlCompat.fromHtml(
                    htmlText,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                ).toString()
            )
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis
    )
}


@Preview(showBackground = true)
@Composable
fun StandardTimelineItemCardPreview() {
    val standardItem = TimelineItem(
        id = PostId("1"),
        content = "Exploring the latest trends in mobile app development. Excited to share insights!",
        createdAt = Instant.now().minus(Duration.ofHours(2)),
        account = Account(
            displayName = "John Doe",
            username = "johndoe",
            avatar = "https://example.com/avatar.jpg"
        )
    )

    TimelineItemCard(
        item = standardItem,
        onDismiss = { /* Preview dismiss action */ }
    )
}

@Preview(showBackground = true)
@Composable
fun LongContentTimelineItemCardPreview() {
    val longContentItem = TimelineItem(
        id = PostId("2"),
        content = "His palms are sweaty, knees weak, arms are heavy\n" +
                "There's vomit on his sweater already, mom's spaghetti\n" +
                "He's nervous, but on the surface, he looks calm and ready\n" +
                "To drop bombs, but he keeps on forgetting\n" +
                "What he wrote down, the whole crowd goes so loud\n" +
                "He opens his mouth, but the words won't come out\n" +
                "He's chokin', how? Everybody's jokin' now\n" +
                "The clock's run out, time's up, over, blaow\n" +
                "Snap back to reality, ope, there goes gravity\n" +
                "Ope, there goes Rabbit, he choked, he's so mad\n" +
                "But he won't give up that easy, no, he won't have it\n" +
                "He knows his whole back's to these ropes, it don't matter\n" +
                "He's dope, he knows that, but he's broke, he's so stagnant\n" +
                "He knows when he goes back to this mobile home, that's when it's\n" +
                "Back to the lab again, yo, this old rhapsody\n" +
                "Better go capture this moment and hope it don't pass him",
        createdAt = Instant.now().minus(Duration.ofDays(1)),
        account = Account(
            displayName = "John Doe",
            username = "johndoe",
            avatar = "https://example.com/avatar.jpg"
        )
    )

    TimelineItemCard(
        item = longContentItem,
        onDismiss = { }
    )
}

@Preview(showBackground = true)
@Composable
fun HtmlFormattedTimelineItemCardPreview() {
    val htmlFormattedItem = TimelineItem(
        id = PostId("3"),
        content = "<b>Key Takeaways:</b><br/>" +
                "• <i>Innovation</i> drives technological progress<br/>" +
                "• <u>Collaboration</u> is essential for success<br/>" +
                "• Continuous learning remains paramount in tech industries",
        createdAt = Instant.now().minus(Duration.ofMinutes(30)),
        account = Account(
            displayName = "John Doe",
            username = "johndoe",
            avatar = "https://example.com/avatar.jpg"
        )
    )

    TimelineItemCard(
        item = htmlFormattedItem,
        onDismiss = { }
    )
}

@Preview(showBackground = true)
@Composable
fun ShortTimelineItemCardPreview() {
    val shortItem = TimelineItem(
        id = PostId("4"),
        content = "Quick update: Project milestone achieved!",
        createdAt = Instant.now(),
        account = Account(
            displayName = "John Doe",
            username = "johndoe",
            avatar = "https://example.com/avatar.jpg"
        )
    )

    TimelineItemCard(
        item = shortItem,
        onDismiss = { }
    )
}

private fun Instant.formatRelative(): String {
    val now = Instant.now()
    val diff = Duration.between(this, now).toKotlinDuration()

    return when {
        diff.inWholeMinutes < 1 -> "just now"
        diff.inWholeHours < 1 -> "${diff.inWholeMinutes}m"
        diff.inWholeDays < 1 -> "${diff.inWholeHours}h"
        else -> "${diff.inWholeDays}d"
    }
}
