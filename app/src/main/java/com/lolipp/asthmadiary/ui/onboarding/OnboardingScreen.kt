package com.lolipp.asthmadiary.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolipp.asthmadiary.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        R.string.onboarding_1_title to R.string.onboarding_1_text,
        R.string.onboarding_2_title to R.string.onboarding_2_text,
        R.string.onboarding_3_title to R.string.onboarding_3_text,
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                val (titleRes, textRes) = pages[page]
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(titleRes),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(textRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (active) 10.dp else 8.dp)
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            drawCircle(
                                color = if (active) {
                                    androidx.compose.ui.graphics.Color(0xFF2196F3)
                                } else {
                                    androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .height(56.dp)
            ) {
                Text(
                    stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                    fontSize = 16.sp
                )
            }
        }
    }
}
