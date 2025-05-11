package com.cloud.bff.services.serviceImpl;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final String authCode = "byXgwrjxzOwXSB9xP5sdHT76UuFsw_GqwkbHnpun2hDVAzFu6ixNXw==";

    public UserServiceImpl(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://apiazuregraphqlresttouserwin.azurewebsites.net/api").build();
        this.objectMapper = objectMapper;
    }
    
    @Override
    public ResponseModel getUsers() {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Get raw JSON response string
            String jsonResponse = webClient.get()
                    .uri("/userRest")
                    .header("x-functions-key", authCode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Manually map fields to handle roleId → rol conversion
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(jsonResponse);
            List<UserModel> users = new ArrayList<>();
            
            for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                UserModel user = new UserModel();
                user.setId(node.has("id") ? node.get("id").asLong() : null);
                user.setEmail(node.has("email") ? node.get("email").asText() : null);
                user.setPassword(node.has("password") ? node.get("password").asText() : null);
                
                // Set roleId directly to rol without any conversion
                if (node.has("roleId") && !node.get("roleId").isNull()) {
                    user.setRol(node.get("roleId").asText());
                }
                
                users.add(user);
            }
            
            responseModel.setData(users);
            responseModel.setMessage("Success");
            responseModel.setStatus(200);
            responseModel.setError(null);

            return responseModel;

        } catch (Exception e) {
            logger.error("Error getting users: {}", e.getMessage(), e);
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel addUser(UserModel user) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Create a clean JSON object with the required fields
            ObjectNode json = objectMapper.createObjectNode();
            json.put("email", user.getEmail());
            json.put("password", user.getPassword());
            
            // Only add roleId if rol is provided in the UserModel
            if (user.getRol() != null && !user.getRol().isEmpty()) {
                try {
                    Long roleId = Long.parseLong(user.getRol());
                    json.put("roleId", roleId);
                } catch (NumberFormatException e) {
                    // If not a valid number, don't include roleId
                    // The event handler will assign default role
                    logger.warn("Invalid role ID format: {}. Default role will be assigned.", user.getRol());
                }
            }

            logger.info("Creating user: {}", json);

            String response = webClient.post()
                    .uri("/userRest")
                    .header("x-functions-key", authCode)
                    .header("Content-Type", "application/json")
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            responseModel.setData(response);
            
            // Check response for errors
            if (response != null && response.contains("Error")) {
                responseModel.setMessage("Failed to create user");
                responseModel.setStatus(500);
                responseModel.setError(response);
            } else {
                responseModel.setMessage("User created successfully");
                responseModel.setStatus(201);
                responseModel.setError(null);
            }

            return responseModel;

        } catch (Exception e) {
            logger.error("Error adding user: {}", e.getMessage(), e);
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel updateUser(UserModel user) {
        ResponseModel responseModel = new ResponseModel();

        try {
            // Create a clean JSON object with the required fields
            ObjectNode json = objectMapper.createObjectNode();
            json.put("id", user.getId());
            json.put("email", user.getEmail());
            json.put("password", user.getPassword());
            
            // Only add roleId if rol is provided in the UserModel
            if (user.getRol() != null && !user.getRol().isEmpty()) {
                try {
                    Long roleId = Long.parseLong(user.getRol());
                    json.put("roleId", roleId);
                } catch (NumberFormatException e) {
                    // If not a valid number, skip roleId to avoid errors
                    logger.warn("Invalid role ID format: {}. Role won't be updated.", user.getRol());
                }
            }

            logger.info("Updating user: {}", json);

            String response = webClient.put()
                    .uri("/userRest")
                    .header("x-functions-key", authCode)
                    .header("Content-Type", "application/json")
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            responseModel.setData(response);
            
            // Check response for errors
            if (response != null && response.contains("Error")) {
                responseModel.setMessage("Failed to update user");
                responseModel.setStatus(500);
                responseModel.setError(response);
            } else {
                responseModel.setMessage("User updated successfully");
                responseModel.setStatus(200);
                responseModel.setError(null);
            }

            return responseModel;

        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel deleteUser(Long id) {
        ResponseModel responseModel = new ResponseModel();

        try {
            logger.info("Deleting user with ID: {}", id);
            
            // Create a payload with the ID for deletion
            ObjectNode deleteBody = objectMapper.createObjectNode();
            deleteBody.put("id", id);
            
            String response = webClient.method(HttpMethod.DELETE)
                    .uri("/userRest")
                    .header("x-functions-key", authCode)
                    .header("Content-Type", "application/json")
                    .bodyValue(deleteBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            responseModel.setData(response);
            
            // Check if user was found and deleted successfully
            if (response != null && response.contains("no encontrado")) {
                responseModel.setMessage("User not found");
                responseModel.setStatus(404);
                responseModel.setError(response);
            } else if (response != null && response.contains("Error")) {
                responseModel.setMessage("Failed to delete user");
                responseModel.setStatus(500);
                responseModel.setError(response);
            } else {
                responseModel.setMessage("User deleted successfully");
                responseModel.setStatus(200);
                responseModel.setError(null);
            }

            return responseModel;

        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage(), e);
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }

    @Override
    public ResponseModel getUserById(Long id) {
        ResponseModel responseModel = new ResponseModel();

        try {
            logger.info("Fetching user with ID: {}", id);
            
            // Get user by ID from the Azure Function
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/userRest")
                            .queryParam("id", id)
                            .build())
                    .header("x-functions-key", authCode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Check if the response is valid
            if (response == null || response.isEmpty() || response.equals("null")) {
                responseModel.setMessage("User not found");
                responseModel.setStatus(404);
                responseModel.setError("No user exists with ID: " + id);
                return responseModel;
            }
            
            // Parse the response to extract user data
            try {
                com.fasterxml.jackson.databind.JsonNode userNode = objectMapper.readTree(response);
                UserModel user = new UserModel();
                user.setId(userNode.has("id") ? userNode.get("id").asLong() : null);
                user.setEmail(userNode.has("email") ? userNode.get("email").asText() : null);
                user.setPassword(userNode.has("password") ? userNode.get("password").asText() : null);
                
                // Set roleId directly to rol without any conversion
                if (userNode.has("roleId") && !userNode.get("roleId").isNull()) {
                    user.setRol(userNode.get("roleId").asText());
                }
                
                responseModel.setData(user);
                responseModel.setMessage("User retrieved successfully");
                responseModel.setStatus(200);
                responseModel.setError(null);
            } catch (Exception e) {
                logger.error("Error parsing user data: {}", e.getMessage(), e);
                responseModel.setMessage("Error parsing user data");
                responseModel.setStatus(500);
                responseModel.setError(e.getMessage());
            }

            return responseModel;

        } catch (Exception e) {
            logger.error("Error getting user by ID: {}", e.getMessage(), e);
            responseModel.setMessage(e.getLocalizedMessage());
            responseModel.setStatus(500);
            responseModel.setError(e.getMessage());

            return responseModel;
        }
    }
}
