package ru.darkrpmc

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import ru.darkrpmc.AccountsTable.lastIPColumn
import kotlin.jvm.optionals.getOrNull

/** Главный класс плагина */
@Plugin(
    id = "proxy",
    name = "Proxy",
    version = "1.0",
    url = "https://darkrpmc.ru",
    description = "Управляет прокси DarkRPMC",
    authors = ["Alpha812"]
)
class Proxy @Inject constructor(private val server: ProxyServer, logger: Logger) {
    /** Сервер лобби */
    private val lobbyServer = server.getServer("lobby").getOrNull()

    /** Сервер авторизации */
    private val authServer = server.getServer("auth").getOrNull()

    // Код исполняемый при инициализации плагина
    init {
        try {
            // Подключение к БД
            Database.connect("jdbc:mysql://localhost:3306/banclaw", user = "Main", password = "151595")
            transaction {
                // Создание таблицы аккаунтов и таблицы игровых данных
                transaction { SchemaUtils.create(AccountsTable, PerServerDataTable) }
            }
        } catch (e: Exception) {
            logger.error("Не удалось подключиться к базе данных: $e")
        }
    }

    /**
     * Код, выполняемой после полной инициализации Velocity:
     * Удаление команд /server и /velocity
     */
    @Subscribe
    fun onInitialize(@Suppress("Unused") e: ProxyInitializeEvent) = server.commandManager.run {
        unregister("server")
        unregister("velocity")
    }

    /**
     * Логика маршрутизации игроков:
     * Если текущий IP игрока совпадает с последним IP авторизации из БД, то игрок сразу попадает в лобби
     */
    @Subscribe
    fun onInitServerChoose(event: PlayerChooseInitialServerEvent) {
        val player = event.player
        val lastAuthIP = transaction {
            AccountsTable
                .select(lastIPColumn)
                .where(AccountsTable.selector(player.username))
                .singleOrNull()
        }?.get(lastIPColumn)
        event.setInitialServer(
            if (lastAuthIP == player.remoteAddress.hostString) lobbyServer else authServer
        )
    }

    /**
     * Логика маршрутизации игроков при исключении игрока с сервера:
     * Если сервер, с которого был исключён игрок не сервер авторизации и не лобби (т.е. игровой сервер),
     * то игрок перенаправляется в лобби
     */
    @Subscribe
    fun onPlayerKick(event: KickedFromServerEvent) {
        if (event.server != authServer && event.server != lobbyServer)
            event.result = KickedFromServerEvent.RedirectPlayer.create(lobbyServer)
    }
}