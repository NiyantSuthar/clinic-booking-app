package com.clinic.booking.repository;

import com.clinic.booking.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByAccount_Id(Long accountId);

    /**
     * JOIN FETCH pulls the Account in the same query rather than one lazy
     * load per beneficiary - matters for GET /admin/today, which needs
     * every beneficiary's phone number (via their Account) in one shot
     * rather than N+1 queries.
     */
    @Query("SELECT b FROM Beneficiary b JOIN FETCH b.account WHERE b.id IN :ids")
    List<Beneficiary> findByIdInWithAccount(@Param("ids") List<Long> ids);
}