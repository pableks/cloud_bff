
# Documentación de API Guía para Endpoints REST y GraphQL
## URL Base

```
http://ip172-18-0-3-d0gisjq91nsg008du29g-8080.direct.labs.play-with-docker.com
```

## API REST

### Endpoints de Roles

#### Obtener todos los roles

- **Método**: GET
- **URL**: `/api/rest?resource=roles`

#### Obtener un rol por ID

- **Método**: GET
- **URL**: `/api/rest?resource=roles&id={id}`

#### Crear un rol

- **Método**: POST
- **URL**: `/api/rest?resource=roles`
- **Body** (JSON):

```json
{
  "title": "Admin",
  "description": "Administrador del sistema"
}
```

#### Actualizar un rol

- **Método**: PUT
- **URL**: `/api/rest?resource=roles`
- **Body** (JSON):

```json
{
  "id": 1,
  "title": "Admin actualizado",
  "description": "Descripción modificada"
}
```

#### Eliminar un rol

- **Método**: DELETE
- **URL**: `/api/rest?resource=roles&id={id}`

### Endpoints de Usuarios

#### Obtener todos los usuarios

- **Método**: GET
- **URL**: `/api/rest?resource=users`

#### Obtener un usuario por ID

- **Método**: GET
- **URL**: `/api/rest?resource=users&id={id}`

#### Crear un usuario

- **Método**: POST
- **URL**: `/api/rest?resource=users`
- **Body** (JSON):

```json
{
  "email": "correo@ejemplo.com",
  "password": "password123",
  "rol": "1"
}
```

#### Actualizar un usuario

- **Método**: PUT
- **URL**: `/api/rest?resource=users`
- **Body** (JSON):

```json
{
  "id": 1,
  "email": "nuevo@ejemplo.com",
  "password": "nueva123",
  "rol": "2"
}
```

#### Eliminar un usuario

- **Método**: DELETE
- **URL**: `/api/rest?resource=users&id={id}`

## API GraphQL

- **URL**: `/api/graphql`
- **Método**: POST
- **Headers**:

```json
{
  "Content-Type": "application/json"
}
```

### Operaciones sobre Roles

#### Consultar todos los roles

```graphql
query {
  roles {
    id
    title
    description
  }
}
```

#### Consultar un rol por ID

```graphql
query {
  role(id: 1) {
    id
    title
    description
  }
}
```

#### Crear un rol

```graphql
mutation {
  createRole(title: "Nuevo Rol", description: "Un nuevo rol") {
    id
    title
  }
}
```

#### Actualizar un rol

```graphql
mutation {
  updateRole(id: 1, title: "Rol actualizado", description: "Descripción nueva") {
    id
    title
    description
  }
}
```

#### Eliminar un rol

```graphql
mutation {
  deleteRole(id: 1)
}
```

### Operaciones sobre Usuarios

#### Consultar todos los usuarios

```graphql
query {
  users {
    id
    email
    rol
  }
}
```

#### Consultar un usuario por ID

```graphql
query {
  user(id: 1) {
    id
    email
    rol
  }
}
```

#### Crear un usuario

```graphql
mutation {
  createUser(email: "usuario@ejemplo.com", password: "password123", rol: "1") {
    id
    email
    rol
  }
}
```

#### Actualizar un usuario

```graphql
mutation {
  updateUser(id: 1, email: "nuevo@ejemplo.com", password: "nueva123", rol: "2") {
    id
    email
    rol
  }
}
```

#### Eliminar un usuario

```graphql
mutation {
  deleteUser(id: 1)
}
```