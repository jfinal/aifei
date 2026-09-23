package exported;

public class ThrowingService {
    public ThrowingService() {
        throw new IllegalStateException("constructor");
    }
}
