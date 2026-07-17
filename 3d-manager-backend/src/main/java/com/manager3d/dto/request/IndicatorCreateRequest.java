package com.manager3d.dto.request;

import com.manager3d.entity.Indicator;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IndicatorCreateRequest {
    @NotBlank
    private String name;
    private String description;
    private String type;       // ATOMIC / DERIVED / COMPOSITE
    private String valueType;  // NUMBER / STRING / JSON / TABLE
    private String value;
    private Long dataSourceId;
    private Long taskId;
    private String tags;
    private String visibility; // PRIVATE / DEPARTMENT_SHARED / PUBLIC

    public Indicator.Type getTypeEnum() {
        return type != null ? Indicator.Type.valueOf(type) : Indicator.Type.ATOMIC;
    }

    public Indicator.ValueType getValueTypeEnum() {
        return valueType != null ? Indicator.ValueType.valueOf(valueType) : Indicator.ValueType.NUMBER;
    }

    public Indicator.Visibility getVisibilityEnum() {
        return visibility != null ? Indicator.Visibility.valueOf(visibility) : Indicator.Visibility.PRIVATE;
    }
}
