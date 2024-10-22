package com.mobilise.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BorrowingReportDTO {
    private Map<String, Long> borrowingCountsByBook;
    private List<BorrowingEventDTO> borrowingEvents;
}