package digital.vasic.yole.network.common

/**
 * Status of network operations.
 */
enum class OperationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}