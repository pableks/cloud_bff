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
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import com.database_connection.OracleDBConnection;
import com.microsoft.azure.functions.*;

@SuppressWarnings("deprecation")
public class RolesGraphQl {
        private static final Logger logger = Logger.getLogger(OracleDBConnection.class.getName());

        @FunctionName("getRolesGraphQL")
        public HttpResponseMessage run(
                        @HttpTrigger(name = "req", methods = {
                                        HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
                        final ExecutionContext context) {

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

                try {
                        // we generate the connection:
                        Connection conn = OracleDBConnection.getConnection();

                        // we prepare the query
                        String selectQuery = "SELECT * FROM ROLES";

                        PreparedStatement preparedStatement = conn.prepareStatement(selectQuery);

                        // we execute the query
                        ResultSet rs = preparedStatement.executeQuery();

                        List<RolModel> rolesFounded = new ArrayList<>();

                        while (rs.next()) {
                                long id = rs.getLong("ID");
                                String title = rs.getString("TITLE");
                                String description = rs.getString("DESCRIPTION");

                                RolModel rol = new RolModel();

                                rol.setId(id);
                                rol.setTitle(title);
                                rol.setDescription(description);

                                rolesFounded.add(rol);
                        }

                        logger.info("Roles found: " + rolesFounded.size());

                        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                                        .name("Query")
                                        .field(field -> field
                                                        .name("roles")
                                                        .type(GraphQLList.list(
                                                                        roleType))
                                                        .dataFetcher(environment -> rolesFounded))
                                        .build();

                        GraphQLSchema schema = GraphQLSchema.newSchema()
                                        .query(queryType)
                                        .build();

                        GraphQL graphql = GraphQL.newGraphQL(schema).build();

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

                } catch (SQLException e) {
                        logger.warning(e.getMessage());
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                                        .body(e.getMessage()).build();
                }

        }

}
