package com.kotlingdgocucb.elimuApp.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.airbnb.lottie.compose.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.kotlingdgocucb.elimuApp.R
import com.kotlingdgocucb.elimuApp.domain.model.User
import com.kotlingdgocucb.elimuApp.data.datasource.local.room.entity.Video
import com.kotlingdgocucb.elimuApp.ui.components.Rating
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import com.kotlingdgocucb.elimuApp.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableTitle(
    title: String,
    maxLength: Int = 25,
    style: TextStyle,
    color: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val displayTitle = if (expanded || title.length <= maxLength) title else title.take(maxLength) + "..."
    Text(
        text = displayTitle,
        style = style,
        color = color,
        maxLines = if (expanded) Int.MAX_VALUE else 1,
        overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
        modifier = Modifier.combinedClickable(
            onClick = { /* Action sur clic simple si nécessaire */ },
            onLongClick = { expanded = !expanded }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    videoViewModel: VideoViewModel = koinViewModel(),
    navController: NavController,
    userInfo: User?
) {
    val videosState = videoViewModel.videos.observeAsState(initial = emptyList())
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        videoViewModel.fetchAllVideos()
    }

    if (videosState.value.isEmpty()) {
        FullScreenLoadingAnimation()
        return
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)
    val sortedVideos = videosState.value.sortedBy { it.order }
    val mentorVideos = sortedVideos.filter { it.mentor_email == userInfo?.mentor_email }
    Log.d("ELIMUMENTOR", "Lecture du mentor: ${userInfo?.mentor_email}")
    val trackVideos = mentorVideos.filter { it.category.equals(userInfo?.track, ignoreCase = true) }

    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = if (searchQuery.isNotEmpty()) {
        trackVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    } else emptyList()

    val filteredVideos = if (searchQuery.isBlank()) trackVideos
    else trackVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }

    val popularVideos = filteredVideos.filter { it.stars > 3.5 }.take(5)
    val recommendedVideos = filteredVideos.filter { !popularVideos.contains(it) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold {
            innerPadding ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    isRefreshing = true
                    videoViewModel.fetchAllVideos()
                    coroutineScope.launch {
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                )
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Barre de recherche moderne
                    item {
                        ModernSearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = {
                                searchQuery = it
                                showSuggestions = it.isNotEmpty()
                            },
                            suggestions = suggestions,
                            showSuggestions = showSuggestions,
                            onSuggestionClick = { suggestion ->
                                searchQuery = suggestion.title
                                showSuggestions = false
                            },
                            onDismissSuggestions = { showSuggestions = false },
                            navController = navController
                        )
                    }

                    // Section des vidéos populaires
                    if (popularVideos.isNotEmpty()) {
                        item {
                            ModernSectionTitle(
                                title = "🔥 Tendances",
                                subtitle = "${popularVideos.size} vidéos populaires",
                                onVoirPlus = { navController.navigate("screenVideoPopulare") }
                            )
                        }
                        
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(popularVideos) { video ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = scaleIn(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) + fadeIn(),
                                        exit = scaleOut() + fadeOut()
                                    ) {
                                        ModernVideoCardPopular(video = video) {
                                            navController.navigate("videoDetail/${video.id}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section des vidéos recommandées
                    if (recommendedVideos.isNotEmpty()) {
                        item {
                            ModernSectionTitle(
                                title = "📚 Pour vous",
                                subtitle = "Recommandé par votre mentor",
                                onVoirPlus = {
                                    navController.navigate("screenVideoTrack/${userInfo?.track}")
                                }
                            )
                        }

                        if (isTablet) {
                            item {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(600.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(recommendedVideos) { video ->
                                        ModernVideoGridItem(video = video) {
                                            navController.navigate("videoDetail/${video.id}")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(recommendedVideos) { video ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = tween(300)
                                    ) + fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    ModernVideoRowItem(
                                        video = video,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        navController.navigate("videoDetail/${video.id}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    suggestions: List<Video>,
    showSuggestions: Boolean,
    onSuggestionClick: (Video) -> Unit,
    onDismissSuggestions: () -> Unit,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                label = {
                    Text(
                        "Rechercher un cours...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Suggestions modernes
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty(),
            enter = slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(suggestions) { suggestion ->
                        ModernSuggestionItem(
                            video = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSuggestionItem(
    video: Video,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            SubcomposeAsyncImage(
                model = "https://img.youtube.com/vi/${video.youtube_url}/default.jpg",
                contentDescription = "Miniature de ${video.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> LottieImageLoadingAnimation()
                    else -> SubcomposeAsyncImageContent()
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cours ${video.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModernSectionTitle(
    title: String,
    subtitle: String? = null,
    onVoirPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        TextButton(
            onClick = onVoirPlus,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Voir tout",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ModernVideoCardPopular(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(300.dp)
            .height(220.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                SubcomposeAsyncImage(
                    model = "https://img.youtube.com/vi/${video.youtube_url}/hqdefault.jpg",
                    contentDescription = "Miniature de ${video.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> LottieImageLoadingAnimation()
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                
                // Badge populaire
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "🔥 Populaire",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 35,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Rating(rating = video.stars)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Vues",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${video.progresses.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernVideoRowItem(
    video: Video,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                SubcomposeAsyncImage(
                    model = "https://img.youtube.com/vi/${video.youtube_url}/hqdefault.jpg",
                    contentDescription = "Miniature de ${video.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> LottieImageLoadingAnimation()
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 45,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Rating(rating = video.stars)
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Cours ${video.order}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${video.progresses.size} vues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = video.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ModernVideoGridItem(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 12f)
            .clickable { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                SubcomposeAsyncImage(
                    model = "https://img.youtube.com/vi/${video.youtube_url}/hqdefault.jpg",
                    contentDescription = "Miniature de ${video.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> LottieImageLoadingAnimation()
                        else -> SubcomposeAsyncImageContent()
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 30,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Rating(rating = video.stars)
                    
                    Text(
                        text = "Cours ${video.order}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenLoadingAnimation() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Chargement des cours...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LottieImageLoadingAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.imageloading)
        )
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(60.dp)
        )
    }
}