package com.touplus.billing_api.admin.repository.impl;

import com.touplus.billing_api.admin.enums.ProcessType;
import com.touplus.billing_api.admin.repository.MessageProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MessageProcessRepositoryImpl implements MessageProcessRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public ProcessType findLatestKafkaReceiveStatus() {
        String sql = """
            SELECT kafka_receive
            FROM billing_message.message_process
        """;


	    return jdbcTemplate.query(
	            sql,
	            (rs, rowNum) -> ProcessType.valueOf(rs.getString("kafka_receive"))
	    ).stream().findFirst().orElse(null);
    }

    @Override
    public ProcessType findLatestCreateMessageStatus() {
        String sql = """
            SELECT create_message
            FROM billing_message.message_process
        """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> ProcessType.valueOf(rs.getString("create_message"))
        ).stream().findFirst().orElse(null);
    }

    @Override
    public ProcessType findLatestSentMessageStatus() {
        String sql = """
            SELECT sent_message
            FROM billing_message.message_process
        """;


        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> ProcessType.valueOf(rs.getString("sent_message"))
        ).stream().findFirst().orElse(null);
    }
}
