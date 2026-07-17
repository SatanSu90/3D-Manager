package com.manager3d.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "analysis_data_source")
public class AnalysisDataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @JsonIgnoreProperties("analysisDataSources")
    private AnalysisTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id", nullable = false)
    @JsonIgnoreProperties({"owner"})
    private DataSource dataSource;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(name = "table_name", length = 200)
    private String tableName;

    @Column(name = "query_sql", columnDefinition = "TEXT")
    private String querySql;
}
