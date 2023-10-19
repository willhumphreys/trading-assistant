package uk.co.threebugs.mochiwhattotrade3.trade;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping("/trades")
    public List<TradeDto> findAll() {

        return tradeService.findAll();
    }

    @PostMapping("/trades/searchByExample")
    public List<TradeDto> findWithExample(
            @RequestBody TradeDto exampleRecord,
            @RequestParam(name = "sortColumn", required = false) String sortColumn,
            @RequestParam(name = "sortDirection", required = false) Sort.Direction sortDirection) {

        var sort = Sort.unsorted();
        if (sortColumn != null && sortDirection != null) {
            sort = Sort.by(sortDirection, sortColumn);
        }

        return tradeService.findTrades(exampleRecord, sort);
    }

}
