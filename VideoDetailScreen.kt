package com.kotlingdgocucb.elimuApp.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.kotlingdgocucb.elimuApp.R
import com.kotlingdgocucb.elimuApp.data.datasource.local.room.entity.ReviewCreate
import com.kotlingdgocucb.elimuApp.ui.components.Rating
import com.kotlingdgocucb.elimuApp.ui.components.RatingBarInput
import com.kotlingdgocucb.elimuApp.ui.viewmodel.MentorViewModel
import com.kotlingdgocucb.elimuApp.ui.viewmodel.ProgressViewModel
import com.kotlingdgocucb.elimuApp.ui.viewmodel.ReviewsViewModel
import com.kotlingdgocucb.elimuApp.ui.viewmodel.VideoViewModel



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableTitle2(
    title: String,
    maxLength: Int = 28,
    style: TextStyle,
    color: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val displayTitle = if (expanded || title.length <= maxLength) title else title.take(maxLength) + "..."
    Text(
        text = displayTitle,
        style = style,
        color = color,
        modifier = Modifier.combinedClickable(
            onClick = { /* Action sur clic simple si nécessaire */ },
            onLongClick = { expanded = !expanded }
        )
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: Int,
    navController: NavController,
    videoViewModel: VideoViewModel = koinViewModel(),
    reviewsViewModel: ReviewsViewModel = koinViewModel(),
    mentorViewModel: MentorViewModel = koinViewModel(),
    progressViewModel: ProgressViewModel = koinViewModel(),
    UserEmail: String
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
    val coroutineScope = rememberCoroutineScope()

    // Chargement de la vidéo et des avis
    LaunchedEffect(videoId) {
        videoViewModel.fetchVideoById(videoId)
        reviewsViewModel.fetchReviews(videoId)
    }

    val video by videoViewModel.videoDetail.observeAsState()
    val reviews by reviewsViewModel.reviews.observeAsState(initial = emptyList())
    val averageRating = if (reviews.isNotEmpty())
        reviews.map { it.stars }.average().toFloat() else 0f

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val mentorsList by mentorViewModel.mentors.observeAsState(initial = emptyList())
    val mentorForVideo = mentorsList!!.find { it.email.equals(video?.mentor_email, ignoreCase = true) }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewComment by remember { mutableStateOf("") }
    var reviewStars by remember { mutableStateOf(5) }
    var isPostingReview by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // Nombre de vues récupéré depuis la liste des progresses
    val viewCount = video?.progresses?.size ?: 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                ModernVideoDetailTopBar(
                    onBackClick = { navController.popBackStack() }
                )
            },
            floatingActionButton = {
                val userHasReviewed = reviews.any { it.menteeEmail.equals(UserEmail, ignoreCase = true) }
                if (!userHasReviewed) {
                    AnimatedVisibility(
                        visible = !userHasReviewed,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { showReviewDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp)
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = "Ajouter un avis",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Donner mon avis",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        ) { padding ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    isRefreshing = true
                    videoViewModel.fetchVideoById(videoId)
                    reviewsViewModel.fetchReviews(videoId)
                    coroutineScope.launch {
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (isPostingReview) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (video == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FullScreenLoadingAnimation()
                            }
                        }
                        Log.d("VideoDetailScreen", "Aucune vidéo trouvée pour l'ID: $videoId")
                    } else {
                        // Titre moderne
                        item {
                            ModernVideoTitle(video = video!!)
                        }
                        
                        // Lecteur vidéo moderne
                        item {
                            ModernVideoPlayer(
                                video = video!!,
                                isPlaying = isPlaying,
                                viewCount = viewCount,
                                onPlayClick = {
                                    isPlaying = true
                                    progressViewModel.trackProgress(videoId, UserEmail)
                                }
                            )
                        }
                        
                        // Statistiques et évaluations
                        if (averageRating > 0f) {
                            item {
                                ModernVideoStats(
                                    video = video!!,
                                    averageRating = averageRating,
                                    viewCount = viewCount
                                )
                            }
                        }
                        
                        // Section des avis
                        item {
                            ModernReviewsSection(reviews = reviews)
                        }
                        
                        // Lien YouTube
                        item {
                            ModernYouTubeLink(
                                video = video!!,
                                clipboardManager = clipboardManager
                            )
                        }
                        
                        // Informations du mentor
                        item {
                            ModernMentorInfo(
                                video = video!!,
                                mentor = mentorForVideo,
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        ModernReviewDialog(
            reviewComment = reviewComment,
            onCommentChange = { reviewComment = it },
            reviewStars = reviewStars,
            onStarsChange = { reviewStars = it },
            onDismiss = { showReviewDialog = false },
            onConfirm = {
                video?.let {
                    isPostingReview = true
                    val reviewCreate = ReviewCreate(
                        videoId = it.id,
                        menteeEmail = UserEmail,
                        stars = reviewStars,
                        comment = reviewComment
                    )
                    reviewsViewModel.sendReview(reviewCreate)
                    isPostingReview = false
                }
                showReviewDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernVideoDetailTopBar(
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            ),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Lecture vidéo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ModernVideoTitle(video: Video) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            ExpandableTitle2(
                title = video.title,
                maxLength = 50,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Cours ${video.order}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = video.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ModernVideoPlayer(
    video: Video,
    isPlaying: Boolean,
    viewCount: Int,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isPlaying) {
                SubcomposeAsyncImage(
                    model = "https://img.youtube.com/vi/${video.youtube_url}/hqdefault.jpg",
                    contentDescription = "Miniature de la vidéo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            LottieAnimation(
                                composition = rememberLottieComposition(
                                    LottieCompositionSpec.RawRes(R.raw.imageloading)
                                ).value,
                                iterations = LottieConstants.IterateForever,
                                modifier = Modifier.size(90.dp)
                            )
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                
                // Bouton play moderne
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    IconButton(
                        onClick = { showReviewDialog = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.play),
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Badge nombre de vues
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Vues",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$viewCount vues",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                YoutubeViewerComponent(videoId = video.youtube_url)
            }
        }
    }
}

@Composable
fun ModernVideoStats(
    video: Video,
    averageRating: Float,
    viewCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📊 Statistiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Note moyenne",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Rating(rating = averageRating)
                    Text(
                        text = "${"%.1f".format(averageRating)}/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$viewCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ModernReviewsSection(reviews: List<com.kotlingdgocucb.elimuApp.data.datasource.local.room.entity.Review>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "💬 Avis des utilisateurs (${reviews.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🤔",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun avis pour le moment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Soyez le premier à donner votre avis !",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reviews) { review ->
                        ModernReviewItem(review = review)
                    }
                }
            }
        }
    }
}

@Composable
fun ModernReviewItem(review: com.kotlingdgocucb.elimuApp.data.datasource.local.room.entity.Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Rating(rating = review.stars.toFloat())
                
                Text(
                    text = review.menteeEmail.substringBefore("@"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            review.comment?.let { comment ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModernYouTubeLink(
    video: Video,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val fullUrl = "https://www.youtube.com/watch?v=${video.youtube_url}"
    var showCopyAnimation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🔗 Lien YouTube",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = fullUrl,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.copy_animation)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullUrl))
                            showCopyAnimation = true
                        }
                    ) {
                        if (showCopyAnimation) {
                            LottieAnimation(
                                composition = composition,
                                iterations = 1,
                                modifier = Modifier.size(24.dp)
                            )
                            LaunchedEffect(showCopyAnimation) {
                                delay(2000)
                                showCopyAnimation = false
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier le lien",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ModernMentorInfo(
    video: Video,
    mentor: com.kotlingdgocucb.elimuApp.data.datasource.local.room.entity.Mentor?,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mentor != null) {
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mentor.profileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image du mentor",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> {
                                LottieAnimation(
                                    composition = rememberLottieComposition(
                                        LottieCompositionSpec.RawRes(R.raw.imageloading)
                                    ).value,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👨‍🏫",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Votre mentor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = mentor?.name ?: video.mentor_email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                mentor?.experience?.let { experience ->
                    Text(
                        text = "$experience ans d'expérience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ModernReviewDialog(
    reviewComment: String,
    onCommentChange: (String) -> Unit,
    reviewStars: Int,
    onStarsChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "✨ Donner mon avis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Votre note :",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                RatingBarInput(
                    rating = reviewStars.toFloat(),
                    onRatingChanged = { newRating -> onStarsChange(newRating.toInt()) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = onCommentChange,
                    label = { Text("Votre commentaire (optionnel)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Publier")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Annuler")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Laisser un avis") },
            text = {
                Column {
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Votre commentaire") },
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingBarInput(
                        rating = reviewStars.toFloat(),
                        onRatingChanged = { newRating -> reviewStars = newRating.toInt() }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        video?.let {
                            isPostingReview = true
                            val reviewCreate = ReviewCreate(
                                videoId = it.id,
                                menteeEmail = UserEmail,
                                stars = reviewStars,
                                comment = reviewComment
                            )
                            reviewsViewModel.sendReview(reviewCreate)
                            isPostingReview = false
                        }
                        showReviewDialog = false
                    }
                ) {
                    Text("Envoyer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
