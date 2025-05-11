package com.graph_ql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import com.microsoft.azure.functions.annotation.*;


import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.database_connection.OracleDBConnection;
import com.microsoft.azure.functions.*;

@SuppressWarnings("deprecation")
public class DeleteRoleGraphQl {
    private static final Logger logger = Logger.getLogger(DeleteRoleGraphQl.class.getName());

    @FunctionName("deleteRoleGraphQL")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        // Define the response type
        GraphQLObjectType deleteResponseType = GraphQLObjectType.newObject()
                .name("DeleteResponse")
                .field(field -> field
                        .name("success")
                        .type(graphql.Scalars.GraphQLBoolean))
                .field(field -> field
                        .name("message")
                        .type(graphql.Scalars.GraphQLString))
                .build();

        // Define the mutation type
        GraphQLObjectType mutationType = GraphQLObjectType.newObject()
                .name("Mutation")
                .field(field -> field
                        .name("deleteRole")
                        .type(deleteResponseType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(graphql.Scalars.GraphQLID)
                                .build())
                        .dataFetcher(environment -> {
                            String idStr = environment.getArgument("id");
                            Long id = Long.parseLong(idStr);
                            Map<String, Object> result = new HashMap<>();

                            try {
                                // Get database connection
                                Connection conn = OracleDBConnection.getConnection();

                                // First check if the role exists
                                String selectQuery = "SELECT * FROM ROLES WHERE ID = ?";
                                PreparedStatement selectStatement = conn.prepareStatement(selectQuery);
                                selectStatement.setLong(1, id);
                                ResultSet rs = selectStatement.executeQuery();
                                
                                if (!rs.next()) {
                                    conn.close();
                                    result.put("success", false);
                                    result.put("message", "Role with ID " + id + " not found");
                                    return result;
                                }

                                // Check if the role is being used by any users
                                String checkUsersQuery = "SELECT COUNT(*) FROM USUARIOS WHERE ROL = ?";
                                PreparedStatement checkStatement = conn.prepareStatement(checkUsersQuery);
                                checkStatement.setLong(1, id);
                                ResultSet checkRs = checkStatement.executeQuery();
                                
                                if (checkRs.next() && checkRs.getInt(1) > 0) {
                                    conn.close();
                                    result.put("success", false);
                                    result.put("message", "Cannot delete role as it is assigned to one or more users");
                                    return result;
                                }

                                // Delete the role
                                String deleteQuery = "DELETE FROM ROLES WHERE ID = ?";
                                PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery);
                                deleteStatement.setLong(1, id);

                                int rowsAffected = deleteStatement.executeUpdate();
                                if (rowsAffected > 0) {
                                    result.put("success", true);
                                    result.put("message", "Role deleted successfully");
                                } else {
                                    result.put("success", false);
                                    result.put("message", "Failed to delete role");
                                }
                                
                                conn.close();
                                return result;
                            } catch (SQLException e) {
                                logger.warning("Error deleting role: " + e.getMessage());
                                result.put("success", false);
                                result.put("message", "Error deleting role: " + e.getMessage());
                                return result;
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