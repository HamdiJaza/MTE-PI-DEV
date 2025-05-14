package tn.esprit.interfaces;
import java.util.List;

public interface ISuivieSP<T>{
    void add(T t);
    List<T> getAll();
    void update(T t);
    void delete(T t);
}
