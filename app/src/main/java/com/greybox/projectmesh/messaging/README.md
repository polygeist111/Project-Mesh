# Messaging Module Documentation

## Overview
The messaging module handles all peer-to-peer messaging functionality in Project Mesh. It provides a structured way to send, receive, and store messages between devices on the local network.

## Package Structure
messaging/
├── data/          # Data layer (entities and DAOs)
├── network/       # Network communication
├── repository/    # Single source of truth for messages
└── ui/           # User interface components

## Key Components 
1. MessageNetworkHandler: Handles network communication for messages 
2. MessageRepository: Central point for message operations 
3. MessageService: Coordinates between network and data layers 
4. ChatScreen: UI for messaging functionality

## Usage Example 
```kotlin 
// Send a message
messageService.sendMessage(address, message)

// Receive messages
viewModel.uiState.collect { state ->
    // Handle messages in UI
}
```
## Flow of Messages 
1. User sends message via ChatScreen 
2. ChatScreenViewModel processes message 
3. MessageService coordinates sending 
4. MessageNetworkHandler handles network communication 
5. Message is stored in local database
