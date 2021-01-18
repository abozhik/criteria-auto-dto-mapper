package abozhik.dto.automapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Selection;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoComponent {

    private DtoContainer rootNode;
    private List<DtoContainer> containerList;
    private Map<String, From<?, ?>> joinMap;
    private Selection<?>[] selection;

}
