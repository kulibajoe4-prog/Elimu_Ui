package com.kotlingdgocucb.elimuApp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.kotlingdgocucb.elimuApp.domain.model.User

data class Contact(val name: String, val email: String)

@Composable
fun MessageScreen(navController: NavController, user: User?) {
    ContactListScreen(navController = navController, user = user)
}

@Composable
fun ContactListScreen(navController: NavController, user: User?) {
    val contacts = listOf(
        Contact("${user?.mentor_name}", "${user?.mentor_email}"),
        Contact("IA", "ia@example.com")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête moderne
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .shadow(
                    elevation = 8.dp,
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
                Text(
                    text = "💬",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Communiquez avec votre mentor et l'IA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        // Liste des contacts moderne
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(contacts) { contact ->
                ModernContactCard(
                    contact = contact,
                    user = user,
                    onClick = {
                        if (contact.name == "${user?.mentor_name}")
                            navController.navigate("mentorScreen")
                        else
                            navController.navigate("iaScreen")
                    }
                )
            }
        }
    }
}

@Composable
fun ModernContactCard(
    contact: Contact,
    user: User?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar moderne
            Card(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (contact.name == "${user?.mentor_name}") {
                    AsyncImage(
                        model = user?.mentor_profileUrl,
                        contentDescription = "Image de profil de ${contact.name}",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🤖",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Informations du contact
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (contact.name == "${user?.mentor_name}") 
                        "Votre mentor personnel" 
                    else "Assistant IA intelligent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Badge de statut
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (contact.name == "${user?.mentor_name}") 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = if (contact.name == "${user?.mentor_name}") 
                            "👨‍🏫 Mentor" 
                        else "🤖 IA",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (contact.name == "${user?.mentor_name}") 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Indicateur de navigation
            Card(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


