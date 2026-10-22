package com.example.tvlauncher.grid

/**
 * Состояние сетки главного экрана.
 *
 * @property order ключи плиток на главном экране, в порядке показа.
 * @property known все ключи, которые лаунчер уже видел установленными: по ним отличаем новое приложение
 *   от того, что пользователь сам убрал с главного экрана.
 * @property fresh ключи с меткой «новое».
 */
data class GridState(
    val order: List<String>,
    val known: Set<String>,
    val fresh: Set<String>,
    val initialized: Boolean,
) {
    companion object {
        val EMPTY = GridState(emptyList(), emptySet(), emptySet(), initialized = false)
    }
}
