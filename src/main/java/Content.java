import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by crassirostris on 2016. 1. 22..
 */
@Data
@AllArgsConstructor(staticName = "create")
public class Content {
    private String title;
    private String link;
}
