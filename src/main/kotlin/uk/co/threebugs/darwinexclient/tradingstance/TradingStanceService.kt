package uk.co.threebugs.darwinexclient.tradingstance

import org.springframework.data.domain.*
import org.springframework.stereotype.*
import uk.co.threebugs.darwinexclient.account.*
import java.nio.file.*

@Service
class TradingStanceService(
    private val tradingStanceFileRepository: TradingStanceFileRepository,
    private val tradingStanceRepository: TradingStanceRepository,
    private val tradingStanceMapper: TradingStanceMapper,
    private val accountRepository: AccountRepository
) {

    fun loadTradingStances(path: Path): List<TradingStanceDto> {
        val tradingStanceFileDtos = tradingStanceFileRepository.load(path)

        return tradingStanceFileDtos.map { tradingStanceFileDto ->
            val account = accountRepository.findByName(tradingStanceFileDto.accountName)!!

            TradingStance(
                symbol = tradingStanceFileDto.symbol,
                direction = tradingStanceFileDto.direction,
                account = account
            )
        }.map {
            tradingStanceRepository.save(it)
        }.map {
            tradingStanceMapper.toDto(it)
        }.toList()

    }

    fun findAll(): List<TradingStanceDto> {
        return this.tradingStanceRepository.findAll().map { tradingStance ->
            tradingStanceMapper.toDto(tradingStance)
        }.toList()
    }

    fun findByAccountName(accountName: String, sort: Sort): List<TradingStanceDto> {
        return this.tradingStanceRepository.findByAccount_Name(accountName, sort).map { tradingStance ->
            tradingStanceMapper.toDto(tradingStance)
        }.toList()
    }

    fun deleteById(id: Int): Int {
        return 1
    }
}