package uk.co.threebugs.darwinexclient

import io.kotest.core.spec.style.StringSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.co.threebugs.darwinexclient.metatrader.TradeEventHandler

@SpringBootTest
@AutoConfigureMockMvc
class MyControllerTest : StringSpec() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var tradeEventHandler: TradeEventHandler

    init {
        "should return data from service when /data is invoked" {
            // Arrange
            //val expectedData = tradeEventHandler.onTick()

            // Act
            val mvcResult = mockMvc.perform(get("/data"))
                .andExpect(status().isOk)
                .andReturn()

            // Assert
            val responseBody = mvcResult.response.contentAsString
            // responseBody shouldBe expectedData
        }
    }
}
