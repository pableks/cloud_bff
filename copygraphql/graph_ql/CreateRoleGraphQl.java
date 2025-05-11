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
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.database_connection.OracleDBConnection;
import com.microsoft.azure.functions.*;

@SuppressWarnings("deprecation")
public class CreateRoleGraphQl {
    private static final Logger logger = Logger.getLogger(CreateRoleGraphQl.class.getName());

    @FunctionName("createRoleGraphQL")
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

        // Define input type for creating a role
        GraphQLInputObjectType roleInputType = GraphQLInputObjectType.newInputObject()
                .name("RoleInput")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("title")
                        .type(graphql.Scalars.GraphQLString)
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("description")
                        .type(graphql.Scalars.GraphQLString)
                        .build())
                .build();

        // Define the mutation type
        GraphQLObjectType mutationType = GraphQLObjectType.newObject()
                .name("Mutation")
                .field(field -> field
                        .name("createRole")
                        .type(roleType)
                        .argument(GraphQLArgument.newArgument()
                                .name("input")
                                .type(roleInputType)
                                .build())
                        .dataFetcher(environment -> {
                            Map<String, Object> input = environment.getArgument("input");
                            String title = (String) input.get("title");
                            String description = (String) input.get("description");

                            try {
                                // Get database connection
                                Connection conn = OracleDBConnection.getConnection();

                                // Insert the new role
                                String insertQuery = "INSERT INTO ROLES (TITLE, DESCRIPTION) VALUES (?, ?)";
                                PreparedStatement preparedStatement = conn.prepareStatement(insertQuery, new String[]{"ID"});
                                preparedStatement.setString(1, title);
                                preparedStatement.setString(2, description);

                                int rowsAffected = preparedStatement.executeUpdate();
                                if (rowsAffected > 0) {
                                    // Get the generated ID
                                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                                    RolModel newRole = new RolModel();
                                    if (generatedKeys.next()) {
                                        newRole.setId(generatedKeys.getLong(1));
                                    }
                                    newRole.setTitle(title);
                                    newRole.setDescription(description);
                                    
                                    conn.close();
                                    return newRole;
                                }
                                
                                conn.close();
                                return null;
                            } catch (SQLException e) {
                                logger.warning("Error creating role: " + e.getMessage());
                                throw new RuntimeException("Error creating role: " + e.getMessage());
                            }
                        }))
                .build();

        // Define an empty query type - required by GraphQL even when only using mutations
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(field -> field
                        .name("_dummy")
                        .type(graphql.Scalars.GraphQLString)
                        .dataFetcher(environment -> ""))
                .build();

        // Create schema with both query and mutation
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)  // This line is important! GraphQL requires a query type
                .mutation(mutationType)
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