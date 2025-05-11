package com.graph_ql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import com.microsoft.azure.functions.annotation.*;
import com.models.RolModel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.database_connection.OracleDBConnection;
import com.microsoft.azure.functions.*;

@SuppressWarnings("deprecation")
public class GetRoleByIdGraphQl {
    private static final Logger logger = Logger.getLogger(GetRoleByIdGraphQl.class.getName());

    @FunctionName("getRoleByIdGraphQL")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        // Define the Role type
        GraphQLObjectType roleType = GraphQLObjectType.newObject()
                .name("Rol")
                .field(field -> field
                        .name("id")
                        .type(graphql.Scalars.GraphQLID))
                .field(field -> field
                        .name("title")
                        .type(graphql.Scalars.GraphQLString))
                .field(field -> field
                        .name("description")
                        .type(graphql.Scalars.GraphQLString))
                .build();

        // Define the query type
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(field -> field
                        .name("role")
                        .type(roleType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(graphql.Scalars.GraphQLID)
                                .build())
                        .dataFetcher(environment -> {
                            String idStr = environment.getArgument("id");
                            Long id = Long.parseLong(idStr);

                            try {
                                // Get database connection
                                Connection conn = OracleDBConnection.getConnection();

                                // Query for the role
                                String selectQuery = "SELECT * FROM ROLES WHERE ID = ?";
                                PreparedStatement preparedStatement = conn.prepareStatement(selectQuery);
                                preparedStatement.setLong(1, id);

                                ResultSet rs = preparedStatement.executeQuery();
                                
                                if (rs.next()) {
                                    long roleId = rs.getLong("ID");
                                    String title = rs.getString("TITLE");
                                    String description = rs.getString("DESCRIPTION");

                                    RolModel role = new RolModel();
                                    role.setId(roleId);
                                    role.setTitle(title);
                                    role.setDescription(description);
                                    
                                    conn.close();
                                    return role;
                                }
                                
                                conn.close();
                                return null;
                            } catch (SQLException e) {
                                logger.warning("Error retrieving role: " + e.getMessage());
                                throw new RuntimeException("Error retrieving role: " + e.getMessage());
                            }
                        }))
                .build();

        // Create schema
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();

        GraphQL graphql = GraphQL.newGraphQL(schema).build();

        try {
            String query = request.getBody().orElse("");

            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query)
                    .build();

            ExecutionResult executionResult = graphql.execute(executionInput);
            Map<String, Object> result = executionResult.toSpecification();

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(result)
                    .build();
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage())
                    .build();
        }
    }
} 