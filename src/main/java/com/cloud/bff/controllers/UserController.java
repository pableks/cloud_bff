package com.cloud.bff.controllers;

import com.cloud.bff.models.ResponseModel;
import com.cloud.bff.models.UserModel;
import com.cloud.bff.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    // Single endpoint that handles all HTTP methods
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseModel handleRequest(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            logger.info("Handling {} request", method);
            
            switch (method) {
                case "GET":
                    return handleGet();
                case "POST":
                    return handlePost(request);
                case "PUT":
                    return handlePut(request);
                case "DELETE":
                    return handleDelete(request);
                default:
                    ResponseModel errorResponse = new ResponseModel();
                    errorResponse.setStatus(405);
                    errorResponse.setMessage("Method not allowed");
                    return errorResponse;
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage(), e);
            ResponseModel errorResponse = new ResponseModel();
            errorResponse.setStatus(500);
            errorResponse.setMessage("Internal server error");
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }
    
    private ResponseModel handleGet() {
        return userService.getUsers();
    }
    
    private ResponseModel handlePost(HttpServletRequest request) throws IOException {
        UserModel user = objectMapper.readValue(request.getInputStream(), UserModel.class);
        return userService.addUser(user);
    }
    
    private ResponseModel handlePut(HttpServletRequest request) throws IOException {
        UserModel user = objectMapper.readValue(request.getInputStream(), UserModel.class);
        return userService.updateUser(user);
    }
    
    private ResponseModel handleDelete(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            try {
                Long id = Long.parseLong(pathInfo.substring(1));
                return userService.deleteUser(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid user ID format");
                return errorResponse;
            }
        }
        
        // Try to get ID from request parameter
        String idParam = request.getParameter("id");
        if (idParam != null) {
            try {
                Long id = Long.parseLong(idParam);
                return userService.deleteUser(id);
            } catch (NumberFormatException e) {
                ResponseModel errorResponse = new ResponseModel();
                errorResponse.setStatus(400);
                errorResponse.setMessage("Invalid user ID format");
                return errorResponse;
            }
        }
        
        ResponseModel errorResponse = new ResponseModel();
        errorResponse.setStatus(400);
        errorResponse.setMessage("User ID is required for DELETE request");
        return errorResponse;
    }
}
