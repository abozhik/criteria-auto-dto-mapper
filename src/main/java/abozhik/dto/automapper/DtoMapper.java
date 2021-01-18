package abozhik.dto.automapper;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoMapper<T> {

    public static final String SEPARATOR = "_";
    public final List<Class<?>> basicTypes = Arrays.asList(String.class, BigDecimal.class, Date.class, java.sql.Date.class, Long.class, Integer.class, Boolean.class, boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class);

    private EntityManager em;
    private CriteriaBuilder criteriaBuilder;
    private Class<T> dtoClass;
    private Class<?> rootClass;
    private List<Predicate> predicates;
    private List<Order> orders;
    private Pageable pageable;

    public DtoMapper(EntityManager em, Class<T> dtoClass, Class<?> rootClass) {
        this.em = em;
        this.dtoClass = dtoClass;
        this.rootClass = rootClass;
    }

    public DtoMapper(EntityManager em, CriteriaBuilder criteriaBuilder, Class<T> dtoClass, Class<?> rootClass, List<Predicate> predicates, List<Order> orders) {
        this.em = em;
        this.criteriaBuilder = criteriaBuilder;
        this.dtoClass = dtoClass;
        this.rootClass = rootClass;
        this.predicates = predicates;
        this.orders = orders;
    }

    public List<T> getDtoList() {
        if (criteriaBuilder == null) {
            criteriaBuilder = em.getCriteriaBuilder();
        }
        CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
        Root<?> root = query.from(rootClass);
        DtoComponent dtoComponent = getDtoComponent(root);
        return getResultList(query, dtoComponent);
    }

    public List<T> getResultList(CriteriaQuery<Tuple> query, DtoComponent dtoComponent) {
        if (criteriaBuilder == null) {
            criteriaBuilder = em.getCriteriaBuilder();
        }
        setSelectionConditionAndOrder(query, criteriaBuilder.tuple(dtoComponent.getSelection()));
        return formList(query, dtoComponent.getRootNode());
    }

    public T getSingleResult(CriteriaQuery<Tuple> query, DtoComponent dtoComponent) {
        if (criteriaBuilder == null) {
            criteriaBuilder = em.getCriteriaBuilder();
        }
        setSelectionConditionAndOrder(query, criteriaBuilder.tuple(dtoComponent.getSelection()));
        return formSingleObject(query, dtoComponent.getRootNode());
    }

    private void setSelectionConditionAndOrder(CriteriaQuery<Tuple> query, CompoundSelection<Tuple> selection) {
        query.select(selection);
        if (CollectionUtils.isNotEmpty(predicates)) {
            query.where(predicates.toArray(new Predicate[0]));
        }
        if (CollectionUtils.isNotEmpty(orders)) {
            query.orderBy(orders);
        }
    }

    private List<T> formList(CriteriaQuery<Tuple> query, DtoContainer rootNode) {
        List<T> resultList = new ArrayList<>();
        Map<String, Object> aliasValueMap = new HashMap<>();
        TypedQuery<Tuple> typedQuery = em.createQuery(query);
        if (pageable != null) {
            typedQuery.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        }
        List<Tuple> tuples = typedQuery.getResultList();
        for (Tuple tuple : tuples) {
            for (TupleElement<?> element : tuple.getElements()) {
                aliasValueMap.put(element.getAlias(), tuple.get(element.getAlias()));
            }
            try {
                Object resultObject = dtoClass.getDeclaredConstructor().newInstance();
                convertToObject(rootNode, resultObject, aliasValueMap);
                resultList.add(dtoClass.cast(resultObject));
            } catch (Exception e) {
                e.printStackTrace();
            }
            aliasValueMap.clear();
        }
        return resultList;
    }

    private T formSingleObject(CriteriaQuery<Tuple> query, DtoContainer rootNode) {
        Map<String, Object> aliasValueMap = new HashMap<>();
        Tuple tuple = em.createQuery(query).getSingleResult();
        for (TupleElement<?> element : tuple.getElements()) {
            aliasValueMap.put(element.getAlias(), tuple.get(element.getAlias()));
        }
        try {
            Object resultObject = dtoClass.getDeclaredConstructor().newInstance();
            convertToObject(rootNode, resultObject, aliasValueMap);
            return dtoClass.cast(resultObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Selection<?>[] getSelection(List<DtoContainer> containerList, Root<?> root) {
        Map<String, From<?, ?>> joinMap = getJoinMap(root, containerList, new HashMap<>(), JoinType.INNER);
        return getSelectionFromJoinMap(containerList, joinMap);
    }

    public Map<String, From<?, ?>> getJoinMap(Root<?> root, List<DtoContainer> containerList, Map<String, JoinType> aliasJoinTypeMap, JoinType typeJoin) {
        Collection<DtoContainer> uniqueContainerList = containerList.stream().filter(distinctByKey(DtoContainer::getAliasPrefix)).collect(Collectors.toList());
        Map<String, From<?, ?>> joinMap = new HashMap<>();
        for (DtoContainer dtoContainer : uniqueContainerList) {
            String key = getExistence(joinMap, dtoContainer);
            JoinType joinType;
            if (typeJoin == null) {
                joinType = aliasJoinTypeMap.get(dtoContainer.getSubObjectName());
                if (joinType == null) {
                    joinType = JoinType.INNER;
                }
            } else {
                joinType = typeJoin;
            }

            if (key != null) {
                From<?, ?> temp = joinMap.get(key);
                joinMap.put(dtoContainer.getAliasPrefix(), dtoContainer.getSubObjectName() != null ? temp.join(dtoContainer.getSubObjectName(), joinType) : temp);
            } else {
                joinMap.put(dtoContainer.getAliasPrefix(), dtoContainer.getSubObjectName() != null ? root.join(dtoContainer.getSubObjectName(), joinType) : root);
            }
        }
        return joinMap;
    }

    public Selection<?>[] getSelectionFromJoinMap(List<DtoContainer> containerList, Map<String, From<?, ?>> joinMap) {
        List<Selection<?>> paths = new ArrayList<>();
        for (DtoContainer container : containerList) {
            if (container.getField() != null) {
                paths.add(joinMap.get(container.getAliasPrefix()).get(container.getColumnName()).alias(container.getAlias()));
            }
        }
        Selection<?>[] selections = new Selection<?>[paths.size()];
        paths.toArray(selections);
        return selections;
    }

    public List<DtoContainer> getSortedListFromRoot(DtoContainer container) {
        List<DtoContainer> containerList = new ArrayList<>();
        getListFromRootNode(container, containerList);

        containerList.sort(Comparator.<DtoContainer, Integer>comparing(item -> StringUtils.countMatches(item.getAlias(), SEPARATOR), Comparator.naturalOrder())
                .thenComparing(DtoContainer::getAlias)
                .thenComparing(item -> item.getDeclaringClass().getTypeName()));
        return containerList;
    }

    public DtoContainer getDtoContainer() {
        List<String> listForAliasForming = new ArrayList<>();
        List<Field> fields = Arrays.asList(dtoClass.getDeclaredFields());
        DtoContainer container = new DtoContainer("", rootClass);
        fillContainer(container, listForAliasForming, fields);
        return container;
    }

    private String getExistence(Map<String, From<?, ?>> joinMap, DtoContainer dtoContainer) {
        String temp = dtoContainer.getAliasPrefix();
        if (temp.contains(SEPARATOR)) {
            temp = temp.substring(0, temp.lastIndexOf(SEPARATOR));
            if (joinMap.get(temp) != null) {
                return temp;
            }
        }
        return null;
    }

    private void fillContainer(DtoContainer container, List<String> listForAliasForming, List<Field> fields) {
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)
                    || Map.class.isAssignableFrom(fieldType)
                    || Modifier.isStatic(field.getModifiers())
                    || fieldType.isAssignableFrom(void.class)
                    || field.isAnnotationPresent(Transient.class)
                    || field.isAnnotationPresent(PostLoad.class)) {
                continue;
            }
            if (!basicTypes.contains(fieldType)) {
                listForAliasForming.add(field.getName());
                container.getChildren().add(new DtoContainer(field.getName(), fieldType));
                fillContainer(container.getChildren().get(container.getChildren().size() - 1), listForAliasForming, Arrays.asList(fieldType.getDeclaredFields()));
                listForAliasForming.remove(listForAliasForming.size() - 1);
                continue;
            }
            field.setAccessible(true);
            if (listForAliasForming.isEmpty()) {
                container.getChildren().add(new DtoContainer(field.getName(), "", field.getName(), field.getDeclaringClass(), null, field));
            } else {
                String join = StringUtils.join(listForAliasForming, SEPARATOR);
                container.getChildren().add(new DtoContainer(field.getName(), join, join + SEPARATOR + field.getName(), field.getDeclaringClass(), listForAliasForming.get(listForAliasForming.size() - 1), field));
            }
        }
    }

    private void getListFromRootNode(DtoContainer rootNode, List<DtoContainer> containerList) {
        for (DtoContainer container : rootNode.getChildren()) {
            containerList.add(container);
            getListFromRootNode(container, containerList);
        }

    }

    private void convertToObject(DtoContainer node, Object resultObject, Map<String, Object> results) {
        for (DtoContainer container : node.getChildren()) {
            try {
                Field containerField = container.getField();
                if (containerField == null) {
                    Field field = resultObject.getClass().getDeclaredField(container.getColumnName());
                    field.setAccessible(true);
                    field.set(resultObject, field.getType().cast(container.getDeclaringClass().getDeclaredConstructor().newInstance()));
                    Object tempObject = field.get(resultObject);
                    convertToObject(container, tempObject, results);
                    if (checkIfAllFieldsAreNull(tempObject)) {
                        field.set(resultObject, null);
                    }
                } else {
                    Class<?> type = containerField.getType();
                    Object obj = results.get(container.getAlias());
                    if (type.isPrimitive()) {
                        if (type.isAssignableFrom(boolean.class))
                            containerField.set(resultObject, Boolean.TRUE.equals(obj));
                    } else {
                        containerField.set(resultObject, type.cast(obj));
                    }
                    convertToObject(container, resultObject, results);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public boolean checkIfAllFieldsAreNull(Object object) throws IllegalAccessException {
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(object) != null) {
                return false;
            }
        }
        return true;
    }

    public DtoComponent getDtoComponent(Root<?> root) {
        DtoContainer rootNode = getDtoContainer();
        List<DtoContainer> containerList = getSortedListFromRoot(rootNode);
        Map<String, From<?, ?>> joinMap = getJoinMap(root, containerList, new HashMap<>(), JoinType.INNER);
        Selection<?>[] selection = getSelectionFromJoinMap(containerList, joinMap);
        return new DtoComponent(rootNode, containerList, joinMap, selection);
    }

    public DtoComponent getDtoComponent(Root<?> root, Map<String, JoinType> aliasJoinTypeMap) {
        DtoContainer rootNode = getDtoContainer();
        List<DtoContainer> containerList = getSortedListFromRoot(rootNode);
        Map<String, From<?, ?>> joinMap = getJoinMap(root, containerList, aliasJoinTypeMap, null);
        Selection<?>[] selection = getSelectionFromJoinMap(containerList, joinMap);
        return new DtoComponent(rootNode, containerList, joinMap, selection);
    }

    public DtoComponent getDtoComponent(Root<?> root, JoinType joinType) {
        DtoContainer rootNode = getDtoContainer();
        List<DtoContainer> containerList = getSortedListFromRoot(rootNode);
        Map<String, From<?, ?>> joinMap = getJoinMap(root, containerList, new HashMap<>(), joinType);
        Selection<?>[] selection = getSelectionFromJoinMap(containerList, joinMap);
        return new DtoComponent(rootNode, containerList, joinMap, selection);
    }

    public static <T> java.util.function.Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}
