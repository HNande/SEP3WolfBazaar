package dk.via.sep3.controller;

import dk.via.sep3.dto.ItemListingJsonDTO;
import dk.via.sep3.dto.ItemListingResponseDTO;
import dk.via.sep3.dto.ItemSearchCriteriaDTO;
import dk.via.sep3.dto.ItemSummaryResponseDTO;
import dk.via.sep3.exception.ApiExceptionHandler;
import dk.via.sep3.exception.InvalidRequestException;
import dk.via.sep3.exception.UpstreamException;
import dk.via.sep3.generated.Category;
import dk.via.sep3.service.ItemListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemListingControllerTest {

    private ItemListingService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ItemListingService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ItemListingController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void getSummariesPassesNormalizedCriteriaAndDefaults() throws Exception {
        when(service.getItemSummaries(any(ItemSearchCriteriaDTO.class))).thenReturn(List.of(
                new ItemSummaryResponseDTO(3, "Coat", "Warm", 20, 2025, 1, 2, 7)));

        mvc.perform(get("/api/item").param("title", "  coat ").param("categories", "CLOTHING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2025))
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].day").value(2));

        ArgumentCaptor<ItemSearchCriteriaDTO> criteria = ArgumentCaptor.forClass(ItemSearchCriteriaDTO.class);
        verify(service).getItemSummaries(criteria.capture());
        assertEquals(new ItemSearchCriteriaDTO("coat", List.of(Category.CLOTHING), 0, 0, 1), criteria.getValue());
    }

    @Test
    void getThumbnailsReturnsDataAndRequiresIds() throws Exception {
        mvc.perform(get("/api/item/thumbnails").param("ids", "2", "3"))
                .andExpect(status().isOk());

        verify(service).getThumbnails(List.of(2, 3));
        mvc.perform(get("/api/item/thumbnails"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'ids'"));
    }

    @Test
    void createAndUpdateValidateListingBodyAndReturnExpectedStatuses() throws Exception {
        mvc.perform(post("/api/item").contentType(MediaType.APPLICATION_JSON).content(validItemBody()))
                .andExpect(status().isCreated());
        verify(service).addItem(any(ItemListingJsonDTO.class));

        mvc.perform(put("/api/item/12").contentType(MediaType.APPLICATION_JSON).content(validItemBody()))
                .andExpect(status().isNoContent());
        verify(service).updateItem(org.mockito.ArgumentMatchers.eq(12), any(ItemListingJsonDTO.class));
    }

    @Test
    void createRejectsInvalidPriceBeforeCallingService() throws Exception {
        String invalid = validItemBody().replace("\"price\":0", "\"price\":-1");

        mvc.perform(post("/api/item").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Price cannot be negative")));

        verifyNoInteractions(service);
    }

    @Test
    void createValidatesNestedImageFields() throws Exception {
        String invalidImage = validItemBody().replace("\"width\":2", "\"width\":0");

        mvc.perform(post("/api/item").contentType(MediaType.APPLICATION_JSON).content(invalidImage))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("width")));

        verifyNoInteractions(service);
    }

    @Test
    void getDeleteAndUpdateDelegateAndMapNotFound() throws Exception {
        when(service.getItemById(12)).thenReturn(new ItemListingResponseDTO(
                12, "Winter Coat", "Warm and waterproof", 25, List.of(), List.of(), 2025, 4, 9, 7));
        mvc.perform(get("/api/item/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.itemName").value("Winter Coat"));
        verify(service).getItemById(12);

        mvc.perform(delete("/api/item/12")).andExpect(status().isNoContent());
        verify(service).removeItem(12);

        when(service.getItemById(99)).thenThrow(new NoSuchElementException("Item 99 not found"));
        mvc.perform(get("/api/item/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item 99 not found"));
    }

    @Test
    void serviceErrorsBecomeBadRequestAndGatewayResponses() throws Exception {
        when(service.getItemSummaries(any(ItemSearchCriteriaDTO.class)))
                .thenThrow(new InvalidRequestException("Counter must be at least 1"));
        mvc.perform(get("/api/item").param("counter", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Counter must be at least 1"));

        when(service.getItemById(50)).thenThrow(new UpstreamException("Tier 3 unavailable", null));
        mvc.perform(get("/api/item/50"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void invalidPathParameterIsReportedAsBadRequest() throws Exception {
        mvc.perform(get("/api/item/not-an-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        verifyNoInteractions(service);
    }

    private static String validItemBody() {
        return """
                {"itemName":"Winter Coat","description":"Warm and waterproof jacket","price":0,
                "categories":["CLOTHING"],"imageList":[{"name":"coat.png","filetype":"PNG",
                "width":2,"height":1,"data":"AQ=="}],"ownerId":7}
                """;
    }
}