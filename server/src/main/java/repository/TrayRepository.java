package repository;

import entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrayRepository extends JpaRepository<Tray, String>
{
}
