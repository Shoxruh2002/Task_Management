package uz.master.demotest.configs.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uz.master.demotest.entity.auth.AuthUser;
import uz.master.demotest.repositories.AuthUserRepository;

import java.util.Optional;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final AuthUserRepository repository;

    public UserDetailsService(AuthUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AuthUser> optional = repository.getAuthUsersByUsernameAndDeletedFalse(username);
        if (optional.isPresent()) {
            return new uz.master.demotest.configs.security.UserDetails(optional.get());

        }
        throw new UsernameNotFoundException("User Not Found");
    }
}
