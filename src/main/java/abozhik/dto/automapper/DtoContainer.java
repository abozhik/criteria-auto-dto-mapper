package abozhik.dto.automapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoContainer {

    private String columnName;
    private String aliasPrefix;
    private String alias;
    private Class<?> declaringClass;
    private String subObjectName;
    private Field field;

    private List<DtoContainer> children = new ArrayList<>();

    public DtoContainer(String columnName, Class<?> declaringClass) {
        this.columnName = columnName;
        this.declaringClass = declaringClass;
        this.alias = "";
        this.aliasPrefix = "";
    }

    public DtoContainer(String columnName, String aliasPrefix, String alias, Class<?> declaringClass, String subObjectName, Field field) {
        this.columnName = columnName;
        this.aliasPrefix = aliasPrefix;
        this.alias = alias;
        this.declaringClass = declaringClass;
        this.subObjectName = subObjectName;
        this.field = field;
    }
}
