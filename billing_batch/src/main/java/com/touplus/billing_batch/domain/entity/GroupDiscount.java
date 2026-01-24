package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDiscount {

    @Id
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "num_of_member", nullable = false)
    private Integer numOfMember;
}
