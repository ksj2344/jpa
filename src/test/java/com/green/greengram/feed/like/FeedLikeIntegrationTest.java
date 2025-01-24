package com.green.greengram.feed.like;

import com.green.greengram.BaseIntegrationTest;
import com.green.greengram.WithAuthUser;
import com.green.greengram.feed.like.model.FeedLikeReq;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //부모타입에게 Transactional이 있기 때문에 실행해도 롤백됨
@WithAuthUser
public class FeedLikeIntegrationTest extends BaseIntegrationTest {
    final String BASE_URL = "/api/feed/like";
    FeedLikeTestCommon common;

    @BeforeAll
    void setUp() {
        common = new FeedLikeTestCommon(objectMapper);
    }

    @Test
    @DisplayName("좋아요 등록")
    //@Rollback(false) //롤백 안할거면 이렇게
    void feedLikeReg() throws Exception {
        final int regSuccessResult = 1;
        final long feedIdNotExisted = 5L;
        feedLikeToggle(regSuccessResult, feedIdNotExisted);
    }

    @Test
    @DisplayName("좋아요 취소")
    void feedLikeCancel() throws Exception {
        final int cancelSuccessResult = 0;
        final long feedIdExisted = 2L;
        feedLikeToggle(cancelSuccessResult, feedIdExisted);
    }

    void feedLikeToggle(final int result, final long feedId) throws Exception {
        ResultActions resultActions = mockMvc.perform(  get(BASE_URL).queryParams(common.getParameter(feedId))  );

        String expectedResJson = common.getExpectedResJson(result);
        resultActions.andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResJson));
    }
}
