package com.prismspace.container.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prismspace.container.ui.theme.*

/**
 * Animated Gradient Box with pulsing effect
 */
@Composable
fun AnimatedGradientBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .background(
                brush = CyanToPurpleGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .graphicsLayer(alpha = alpha)
    ) {
        content()
    }
}

/**
 * Expandable List Item
 */
@Composable
fun ExpandableListItem(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.graphicsLayer(
                    rotationZ = if (expanded) 90f else 0f
                )
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Divider(color = OutlineLight, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

/**
 * Swipeable Action Item
 */
@Composable
fun SwipeActionItem(
    title: String,
    subtitle: String? = null,
    actionText: String = "Delete",
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var actionVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .clickable { actionVisible = !actionVisible }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = actionVisible,
                enter = slideInHorizontally(animationSpec = tween(300)),
                exit = slideOutHorizontally(animationSpec = tween(300))
            ) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(min = 80.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Tab Row with gradient indicator
 */
@Composable
fun GradientTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedTabIndex
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            color = if (isSelected) SurfaceVariant else Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryLight else TextSecondary
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(20.dp)
                                    .background(
                                        brush = CyanToPurpleGradient,
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        Divider(
            color = OutlineLight,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Loading Skeleton with shimmer effect
 */
@Composable
fun ShimmerLoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Int = 100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                color = SurfaceVariant.copy(alpha = shimmerAlpha),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

/**
 * Floating Action Button with gradient
 */
@Composable
fun GradientFAB(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extended: Boolean = false,
    text: String? = null
) {
    if (extended && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            icon = icon,
            text = {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier
                .background(
                    brush = CyanToPurpleGradient,
                    shape = RoundedCornerShape(16.dp)
                ),
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            icon()
        }
    }
}

/**
 * Info Card with icon and action
 */
@Composable
fun InfoCard(
    title: String,
    message: String,
    icon: @Composable () -> Unit,
    actionText: String? = null,
    onAction: () -> Unit = {},
    backgroundColor: Color = SecondaryContainer,
    modifier: Modifier = Modifier
) {
    PrismCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }

            if (actionText != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryLight
                    )
                }
            }
        }
    }
}

