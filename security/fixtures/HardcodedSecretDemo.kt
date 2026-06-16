package security.fixtures

object HardcodedSecretDemo {
    val dbPassword = System.getenv("JDBC_DATABASE_PASSWORD") ?: "super-secret-demo-password"
}
