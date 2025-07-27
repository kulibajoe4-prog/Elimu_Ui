package com.kotlingdgocucb.elimuApp.ui

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.kotlingdgocucb.elimuApp.ui.components.Rating // Assuming Rating is a custom composable for stars
import androidx.compose.material.icons.filled.AccountCircle // For user avatar placeholder
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import com.kotlingdgocucb.elimuApp.ui.viewmodel.VideoViewModel

/**
 * Composable permettant d'afficher un titre tronqué (maxLength lettres).
 * L'utilisateur peut maintenir l'appui sur le titre pour basculer entre l'affichage complet et tronqué.
 * @param title Le titre à afficher.
 * @param maxLength La longueur maximale du titre avant troncation.
 * @param style Le style de texte à appliquer.
 * @param color La couleur du texte.
 */
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
        maxLines = if (expanded) Int.MAX_VALUE else 1, // Permet plusieurs lignes lorsque étendu
        overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis, // Ellipsis lorsque non étendu
        modifier = Modifier.combinedClickable(
            onClick = { /* Action sur clic simple si nécessaire */ },
            onLongClick = { expanded = !expanded } // Bascule l'état d'expansion
        )
    )
}

/**
 * Écran principal des cours, affichant les vidéos populaires et recommandées.
 * Intègre une barre de recherche, une barre supérieure et une barre de navigation inférieure.
 * @param videoViewModel Le ViewModel pour récupérer les données vidéo.
 * @param navController Le contrôleur de navigation pour la navigation entre les écrans.
 * @param userInfo Les informations de l'utilisateur connecté.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(
    videoViewModel: VideoViewModel = koinViewModel(),
    navController: NavController,
    userInfo: User?
) {
    // Observer la liste des vidéos depuis le ViewModel
    val videosState = videoViewModel.videos.observeAsState(initial = emptyList())
    var isRefreshing by remember { mutableStateOf(false) }

    // Déclenche le chargement des vidéos au démarrage de l'écran
    LaunchedEffect(Unit) {
        videoViewModel.fetchAllVideos()
    }

    // Affiche une animation de chargement en plein écran si les vidéos ne sont pas encore chargées
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

    // Filtre les suggestions de recherche basées sur la requête
    val suggestions = if (searchQuery.isNotEmpty()) {
        trackVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    } else emptyList()

    // Filtre les vidéos affichées en fonction de la recherche
    val filteredVideos = if (searchQuery.isBlank()) trackVideos
    else trackVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }

    // Sélectionne les vidéos populaires (celles avec plus de 3.5 étoiles, limitées à 5)
    val popularVideos = filteredVideos.filter { it.stars > 3.5 }.take(5)
    // Sélectionne les vidéos recommandées (les autres vidéos non populaires)
    val recommendedVideos = filteredVideos.filter { !popularVideos.contains(it) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Utilise la couleur de fond du thème Material 3
    ) {
        Scaffold(


        ) { innerPadding ->
            // Composant SwipeRefresh pour rafraîchir le contenu en tirant vers le bas
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    isRefreshing = true
                    videoViewModel.fetchAllVideos()
                    coroutineScope.launch {
                        delay(1000) // Simule un délai réseau
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // Applique le rembourrage du Scaffold
                ) {
                    // Barre de recherche
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showSuggestions = it.isNotEmpty()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        label = {
                            Text(
                                "Chercher un cours",
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Couleur du libellé
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(28.dp), // Coins plus arrondis pour un look moderne
                        singleLine = true,
                        maxLines = 1,
//                        colors = TextFieldDefaults.outlinedTextFieldColors(
//                            focusedBorderColor = MaterialTheme.colorScheme.primary,
//                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                            cursorColor = MaterialTheme.colorScheme.primary
//                        )
                    )

                    // Suggestions de recherche (affichées dans une Card)
                    if (showSuggestions && suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                            elevation = CardDefaults.cardElevation(4.dp), // Élévation pour l'ombre
                            shape = RoundedCornerShape(8.dp) // Coins arrondis pour la carte
                        ) {
                            LazyColumn {
                                items(suggestions) { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = suggestion.title
                                                showSuggestions = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp), // Rembourrage vertical augmenté
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Miniature de la suggestion avec animation de chargement
                                        SubcomposeAsyncImage(
                                            model = "https://img.youtube.com/vi/${suggestion.youtube_url}/default.jpg",
                                            contentDescription = "Miniature de ${suggestion.title}",
                                            modifier = Modifier
                                                .size(56.dp) // Miniature légèrement plus grande
                                                .clip(RoundedCornerShape(8.dp)), // Coins arrondis pour l'image
                                            contentScale = ContentScale.Crop
                                        ) {
                                            when (painter.state) {
                                                is AsyncImagePainter.State.Loading -> LottieImageLoadingAnimation()
                                                else -> SubcomposeAsyncImageContent()
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = suggestion.title,
                                            color = MaterialTheme.colorScheme.onSurface, // Utilise onSurface pour le texte
                                            style = MaterialTheme.typography.bodyLarge // Texte plus grand pour les suggestions
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section des vidéos populaires
                    SectionTitle(
                        title = "Populaires",
                        onVoirPlus = { navController.navigate("screenVideoPopulare") },
                        textColor = MaterialTheme.colorScheme.onSurface // Couleur de texte cohérente
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // Espacement augmenté
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(popularVideos) { video ->
                            VideoCardPopular(video = video) {
                                navController.navigate("videoDetail/${video.id}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // Plus d'espace entre les sections

                    // Section des vidéos recommandées
                    if (recommendedVideos.isNotEmpty()) {
                        SectionTitle(
                            title = "Pour vous",
                            onVoirPlus = {
                                navController.navigate("screenVideoTrack/${userInfo?.track}")
                            },
                            textColor = MaterialTheme.colorScheme.onSurface // Couleur de texte cohérente
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isTablet) {
                            // Grille pour les tablettes
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp), // Espacement augmenté
                                horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacement augmenté
                            ) {
                                items(recommendedVideos) { video ->
                                    VideoGridItem(video = video) {
                                        navController.navigate("videoDetail/${video.id}")
                                    }
                                }
                            }
                        } else {
                            // Liste verticale pour les téléphones
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp), // Espacement augmenté
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(recommendedVideos) { video ->
                                    VideoRowItem(video = video) {
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

/** Animation Lottie de chargement en plein écran */
@Composable
fun FullScreenLoadingAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.loading)
        )
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(150.dp)
        )
    }
}

/**
 * Titre de section avec un bouton "Voir tout".
 * @param title Le titre de la section.
 * @param onVoirPlus L'action à effectuer lorsque le bouton "Voir tout" est cliqué.
 * @param textColor La couleur du texte du titre.
 */
@Composable
fun SectionTitle(
    title: String,
    onVoirPlus: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge, // Titre plus grand
            fontWeight = FontWeight.Bold, // Titre plus gras
            color = textColor
        )
        TextButton(onClick = onVoirPlus) { // Utilise TextButton pour un aspect Material 3
            Text(
                text = "Voir tout", // Changé "voir plus..." en "Voir tout" pour plus de clarté
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Carte "Populaire" pour une vidéo, affichée dans une LazyRow.
 * @param video La vidéo à afficher.
 * @param onClick L'action à effectuer lorsque la carte est cliquée.
 */
@Composable
fun VideoCardPopular(video: Video, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp), // Coins plus arrondis
        modifier = Modifier
            .width(280.dp) // Largeur légèrement ajustée pour mieux s'adapter dans LazyRow
            .height(200.dp) // Hauteur augmentée pour mieux accueillir le contenu
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Ajout d'élévation pour l'ombre
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp) // Hauteur d'image augmentée
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) // Clip les coins supérieurs
                    .background(MaterialTheme.colorScheme.surfaceContainer) // Fond de remplacement
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
            Column(modifier = Modifier.padding(8.dp)) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 30, // Longueur maximale augmentée pour les titres populaires
                    style = MaterialTheme.typography.titleSmall, // Titre plus grand pour les cartes populaires
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Ligne d'affichage des étoiles suivie du nombre de vues
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Rating(rating = video.stars)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, // Utilise onSurfaceVariant pour les icônes
                        modifier = Modifier.size(18.dp) // Icône légèrement plus grande
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${video.progresses.size} vues", // Ajout de "vues"
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Ligne d'affichage du numéro de cours et de la catégorie avec icônes
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Icône vidéo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cours ${video.order}", // Texte simplifié
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Catégorie",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Élément d'une liste (pour téléphone) : miniature et texte à droite.
 * Encapsulé dans une Card pour un style Material 3 cohérent.
 * @param video La vidéo à afficher.
 * @param onClick L'action à effectuer lorsque l'élément est cliqué.
 */
@Composable
fun VideoRowItem(video: Video, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp), // Coins plus arrondis
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp) // Hauteur augmentée pour un meilleur affichage du contenu
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Ajout d'élévation
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp) // Largeur ajustée pour l'image
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) // Clip les coins gauches
                    .background(MaterialTheme.colorScheme.surfaceContainer) // Fond de remplacement
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
            Spacer(modifier = Modifier.width(12.dp)) // Espacement augmenté
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp), // Ajoute un rembourrage à droite
                verticalArrangement = Arrangement.Center
            ) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 40, // Longueur maximale augmentée pour les éléments de ligne
                    style = MaterialTheme.typography.titleMedium, // Titre plus grand
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Ligne d'affichage des étoiles suivie du nombre de vues
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Rating(rating = video.stars)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${video.progresses.size} vues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Ligne d'affichage du numéro de cours et de la catégorie
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Icône vidéo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cours ${video.order}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Catégorie",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Élément d'une grille (pour tablette) pour une vidéo.
 * Encapsulé dans une Card pour un style Material 3 cohérent.
 * @param video La vidéo à afficher.
 * @param onClick L'action à effectuer lorsque l'élément est cliqué.
 */
@Composable
fun VideoGridItem(video: Video, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp), // Coins plus arrondis
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f) // Maintient le rapport d'aspect pour la miniature vidéo
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Ajout d'élévation
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) // Clip les coins supérieurs
                    .background(MaterialTheme.colorScheme.surfaceContainer) // Fond de remplacement
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
            Column(modifier = Modifier.padding(8.dp)) {
                ExpandableTitle(
                    title = video.title,
                    maxLength = 30, // Longueur maximale augmentée
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Affichage du numéro de cours et de la catégorie avec icônes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Icône vidéo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cours ${video.order}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Catégorie",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Évaluation et vues pour l'élément de grille
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Rating(rating = video.stars)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Vues",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${video.progresses.size} vues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Animation Lottie pour le chargement d'une image miniature */
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
            modifier = Modifier.size(80.dp)
        )
    }
}
