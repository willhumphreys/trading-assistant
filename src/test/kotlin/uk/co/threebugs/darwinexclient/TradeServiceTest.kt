import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.co.threebugs.darwinexclient.DarwinexClientApplication
import uk.co.threebugs.darwinexclient.trade.TradeService

@Suppress("LeakingThis")
@SpringBootTest(classes = [DarwinexClientApplication::class])
class TradeServiceTest : FunSpec() {


    @Autowired
    private lateinit var tradeService: TradeService

    init {
        extension(SpringExtension)

        test("Get user by id should return the user") {

            val result = tradeService.findAll()

            result shouldNotBe null
        }
    }
}