package com.kotlingdgocucb.elimuApp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.kotlingdgocucb.elimuApp.R
import com.kotlingdgocucb.elimuApp.domain.model.User
import com.kotlingdgocucb.elimuApp.domain.utils.AppDestinations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    userInfo: User?,
    notificationsCount: Int,
    onSigninOutClicked: () -> Unit,
    navController: NavController
) {
    var startAnimation by remember { mutableStateOf(false) }
    val textOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else -50f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val alphaValue by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
    )
    LaunchedEffect(Unit) {
        delay(300)
        startAnimation = true
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.Accueil) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerContent = {
                ModernDrawerContent(
                    userInfo = userInfo,
                    textOffset = textOffset,
                    alphaValue = alphaValue,
                    navController = navController,
                    onDestinationClicked = { destination ->
                        coroutineScope.launch { drawerState.close() }
                        currentDestination = destination
                        navController.navigate(destination.route)
                    },
                    onLogoutClicked = { showLogoutDialog = true },
                    onSigninOutClicked = onSigninOutClicked
                )
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    ModernTopAppBar(
                        userInfo = userInfo,
                        notificationsCount = notificationsCount,
                        textOffset = textOffset,
                        alphaValue = alphaValue,
                        navController = navController
                    )
                },
                content = { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Column {
                            NavigationSuiteScaffold(
                                modifier = Modifier.height(900.dp),
                                navigationSuiteItems = {
                                    AppDestinations.entries.forEach {
                                        item(
                                            icon = {
                                                Icon(it.icon, contentDescription = it.contentDescription, modifier = Modifier.size(20.dp))
                                            },
                                            label = { Text(it.label, style = MaterialTheme.typography.labelSmall) },
                                            selected = it == currentDestination,
                                            onClick = {
                                                currentDestination = it
                                            }
                                        )
                                    }
                                }
                            ) {
                                when (currentDestination) {
                                    AppDestinations.Accueil -> CourseScreen(
                                        navController = navController,
                                        userInfo = userInfo
                                    )
                                    AppDestinations.Message -> MessageScreen(
                                        navController = navController,
                                        user = userInfo)
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = onSigninOutClicked
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(
    userInfo: User?,
    notificationsCount: Int,
    textOffset: Float,
    alphaValue: Float,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo de profil avec animation
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer(
                        translationY = textOffset,
                        alpha = alphaValue
                    ),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userInfo?.profile_picture_uri)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.account),
                    contentDescription = "Photo de profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Message de bienvenue moderne
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(
                        translationY = textOffset,
                        alpha = alphaValue
                    )
            ) {
                Text(
                    text = "Bonjour 👋",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TypewriterText(
                    text = userInfo?.name ?: "Invité",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                )
            }
            
            // Bouton notification moderne
            Card(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (notificationsCount > 0) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                IconButton(
                    onClick = { navController.navigate("notifications") },
                    modifier = Modifier.fillMaxSize()
                ) {
                    BadgedBox(
                        badge = {
                            AnimatedVisibility(
                                visible = notificationsCount > 0,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = notificationsCount.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (notificationsCount > 0) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernDrawerContent(
    userInfo: User?,
    textOffset: Float,
    alphaValue: Float,
    navController: NavController,
    onDestinationClicked: (AppDestinations) -> Unit,
    onSigninOutClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.85f),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // En-tête profil moderne
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer(
                                translationY = textOffset,
                                alpha = alphaValue
                            ),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userInfo?.profile_picture_uri)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.account),
                            contentDescription = "Photo de profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = userInfo?.name ?: "Nom utilisateur",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = userInfo?.email ?: "email@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "📚 ${userInfo?.track ?: "Non défini"}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Text(
                        text = "👨‍🏫 ${userInfo?.mentor_name ?: "Non défini"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Items de navigation modernes
            ModernNavigationDrawerItem(
                icon = Icons.Default.Feedback,
                label = "Feedbacks",
                onClick = { navController.navigate("feedback") }
            )
            
            ModernNavigationDrawerItem(
                icon = Icons.Default.Description,
                label = "Terms & Conditions",
                onClick = { navController.navigate("terms") }
            )
            
            ModernNavigationDrawerItem(
                icon = Icons.Default.Info,
                label = "À propos",
                onClick = { navController.navigate("about") }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bouton de déconnexion moderne
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                NavigationDrawerItem(
                    icon = { 
                        Icon(
                            Icons.Default.ExitToApp, 
                            contentDescription = "Se déconnecter",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        ) 
                    },
                    label = { 
                        Text(
                            "Se déconnecter",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    selected = false,
                    onClick = onLogoutClicked,
                    modifier = Modifier.padding(8.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun ModernNavigationDrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { 
            Icon(
                icon, 
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        label = { 
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            ) 
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000)
            onConfirm()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isLoading) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.logout_animation)
                )
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Se déconnecter ?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Êtes-vous sûr de vouloir vous déconnecter ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Annuler")
                    }
                    
                    Button(
                        onClick = { isLoading = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            "Confirmer",
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            } else {
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
                    text = "Déconnexion en cours...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
    typeSpeed: Long = 100L,
    waitEnd: Long = 1500L
) {
    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        while (true) {
            displayedText = ""
            for (char in text) {
                displayedText += char
                delay(typeSpeed)
            }
            delay(waitEnd)
        }
    }
    Text(
        text = displayedText,
        style = style,
        modifier = modifier,
        textAlign = TextAlign.Start
    )
}