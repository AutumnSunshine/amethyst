package com.vitorpamplona.amethyst.ui.screen.loggedIn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.NostrVideoDataSource
import com.vitorpamplona.amethyst.service.model.FileHeaderEvent
import com.vitorpamplona.amethyst.service.model.FileStorageHeaderEvent
import com.vitorpamplona.amethyst.ui.actions.NewPostView
import com.vitorpamplona.amethyst.ui.components.ObserveDisplayNip05Status
import com.vitorpamplona.amethyst.ui.note.FileHeaderDisplay
import com.vitorpamplona.amethyst.ui.note.FileStorageHeaderDisplay
import com.vitorpamplona.amethyst.ui.note.LikeReaction
import com.vitorpamplona.amethyst.ui.note.NoteAuthorPicture
import com.vitorpamplona.amethyst.ui.note.NoteDropDownMenu
import com.vitorpamplona.amethyst.ui.note.NoteUsernameDisplay
import com.vitorpamplona.amethyst.ui.note.RenderRelay
import com.vitorpamplona.amethyst.ui.note.ViewCountReaction
import com.vitorpamplona.amethyst.ui.note.ZapReaction
import com.vitorpamplona.amethyst.ui.screen.FeedEmpty
import com.vitorpamplona.amethyst.ui.screen.FeedError
import com.vitorpamplona.amethyst.ui.screen.FeedState
import com.vitorpamplona.amethyst.ui.screen.FeedViewModel
import com.vitorpamplona.amethyst.ui.screen.LoadingFeed
import com.vitorpamplona.amethyst.ui.screen.NostrVideoFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.ScrollStateKeys
import com.vitorpamplona.amethyst.ui.screen.rememberForeverPagerState
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.amethyst.ui.theme.onBackgroundColorFilter
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.collections.immutable.ImmutableList

@Composable
fun VideoScreen(
    videoFeedView: NostrVideoFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val lifeCycleOwner = LocalLifecycleOwner.current

    WatchAccountForVideoScreen(videoFeedView = videoFeedView, accountViewModel = accountViewModel)

    DisposableEffect(accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Video Start")
                NostrVideoDataSource.start()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(vertical = 0.dp)
        ) {
            SaveableFeedState(
                videoFeedView = videoFeedView,
                accountViewModel = accountViewModel,
                nav = nav,
                scrollStateKey = ScrollStateKeys.VIDEO_SCREEN
            )
        }
    }
}

@Composable
fun WatchAccountForVideoScreen(videoFeedView: NostrVideoFeedViewModel, accountViewModel: AccountViewModel) {
    val accountState by accountViewModel.accountLiveData.observeAsState()

    LaunchedEffect(accountViewModel, accountState?.account?.defaultStoriesFollowList) {
        NostrVideoDataSource.resetFilters()
        videoFeedView.checkKeysInvalidateDataAndSendToTop()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaveableFeedState(
    videoFeedView: NostrVideoFeedViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit,
    scrollStateKey: String? = null
) {
    val pagerState = if (scrollStateKey != null) {
        rememberForeverPagerState(scrollStateKey)
    } else {
        remember { PagerState() }
    }

    WatchScrollToTop(videoFeedView, pagerState)

    RenderPage(videoFeedView, accountViewModel, pagerState, nav)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun WatchScrollToTop(
    viewModel: FeedViewModel,
    pagerState: PagerState
) {
    val scrollToTop by viewModel.scrollToTop.collectAsState()

    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0 && viewModel.scrolltoTopPending) {
            pagerState.scrollToPage(page = 0)
            viewModel.sentToTop()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RenderPage(
    videoFeedView: NostrVideoFeedViewModel,
    accountViewModel: AccountViewModel,
    pagerState: PagerState,
    nav: (String) -> Unit
) {
    val feedState by videoFeedView.feedContent.collectAsState()

    Box() {
        Column {
            Crossfade(
                targetState = feedState,
                animationSpec = tween(durationMillis = 100)
            ) { state ->
                when (state) {
                    is FeedState.Empty -> {
                        FeedEmpty {}
                    }

                    is FeedState.FeedError -> {
                        FeedError(state.errorMessage) {}
                    }

                    is FeedState.Loaded -> {
                        SlidingCarousel(
                            state.feed,
                            pagerState,
                            accountViewModel,
                            nav
                        )
                    }

                    is FeedState.Loading -> {
                        LoadingFeed()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidingCarousel(
    feed: MutableState<ImmutableList<Note>>,
    pagerState: PagerState,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    VerticalPager(
        pageCount = feed.value.size,
        state = pagerState,
        beyondBoundsPageCount = 1,
        modifier = Modifier.fillMaxSize(1f),
        key = { index ->
            feed.value.getOrNull(index)?.idHex ?: "$index"
        }
    ) { index ->
        feed.value.getOrNull(index)?.let { note ->
            RenderVideoOrPictureNote(note, accountViewModel, nav)
        }
    }
}

@Composable
private fun RenderVideoOrPictureNote(
    note: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column(remember { Modifier.fillMaxSize(1f) }) {
        Row(remember { Modifier.weight(1f) }, verticalAlignment = Alignment.CenterVertically) {
            val noteEvent = remember { note.event }
            if (noteEvent is FileHeaderEvent) {
                FileHeaderDisplay(note, accountViewModel)
            } else if (noteEvent is FileStorageHeaderEvent) {
                FileStorageHeaderDisplay(note, accountViewModel)
            }
        }
    }

    Row(verticalAlignment = Alignment.Bottom, modifier = remember { Modifier.fillMaxSize(1f) }) {
        Column(remember { Modifier.weight(1f) }) {
            RenderAuthorInformation(note, nav, accountViewModel)
        }

        Column(
            remember {
                Modifier
                    .width(65.dp)
                    .padding(bottom = 10.dp)
            },
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                ReactionsColumn(note, accountViewModel, nav)
            }
        }
    }
}

@Composable
private fun RenderAuthorInformation(
    note: Note,
    nav: (String) -> Unit,
    accountViewModel: AccountViewModel
) {
    Row(remember { Modifier.padding(10.dp) }, verticalAlignment = Alignment.Bottom) {
        Column(remember { Modifier.size(55.dp) }, verticalArrangement = Arrangement.Center) {
            NoteAuthorPicture(note, nav, accountViewModel, 55.dp)
        }

        Column(
            remember {
                Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .height(65.dp)
                    .weight(1f)
            },
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoteUsernameDisplay(note, remember { Modifier.weight(1f) })
                VideoUserOptionAction(note, accountViewModel)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ObserveDisplayNip05Status(
                    remember { note.author!! },
                    remember { Modifier.weight(1f) }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                RelayBadges(baseNote = note, accountViewModel, nav)
            }
        }
    }
}

@Composable
private fun VideoUserOptionAction(
    note: Note,
    accountViewModel: AccountViewModel
) {
    val popupExpanded = remember { mutableStateOf(false) }
    val enablePopup = remember {
        { popupExpanded.value = true }
    }

    IconButton(
        modifier = remember { Modifier.size(22.dp) },
        onClick = enablePopup
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            null,
            modifier = remember { Modifier.size(20.dp) },
            tint = MaterialTheme.colors.placeholderText
        )

        NoteDropDownMenu(
            note,
            popupExpanded,
            accountViewModel
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelayBadges(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val noteRelaysState by baseNote.live().relays.observeAsState()
    val noteRelays = remember(noteRelaysState) {
        noteRelaysState?.note?.relays ?: emptySet()
    }

    FlowRow() {
        noteRelays.forEach { dirtyUrl ->
            RenderRelay(dirtyUrl, accountViewModel, nav)
        }
    }
}

@Composable
fun ReactionsColumn(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    var wantsToReplyTo by remember {
        mutableStateOf<Note?>(null)
    }

    var wantsToQuote by remember {
        mutableStateOf<Note?>(null)
    }

    if (wantsToReplyTo != null) {
        NewPostView({ wantsToReplyTo = null }, wantsToReplyTo, null, accountViewModel, nav)
    }

    if (wantsToQuote != null) {
        NewPostView({ wantsToQuote = null }, null, wantsToQuote, accountViewModel, nav)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 75.dp, end = 20.dp)) {
        /*
        ReplyReaction(baseNote, accountViewModel, iconSize = 40.dp) {
            wantsToReplyTo = baseNote
        }
        BoostReaction(baseNote, accountViewModel, iconSize = 40.dp) {
            wantsToQuote = baseNote
        }*/
        LikeReaction(baseNote, grayTint = MaterialTheme.colors.onBackground, accountViewModel, nav, iconSize = 40.dp, heartSize = Size35dp, 28.sp)
        ZapReaction(baseNote, grayTint = MaterialTheme.colors.onBackground, accountViewModel, iconSize = 40.dp, animationSize = Size35dp)
        ViewCountReaction(baseNote, grayTint = MaterialTheme.colors.onBackground, barChartSize = 39.dp, viewCountColorFilter = MaterialTheme.colors.onBackgroundColorFilter)
    }
}
