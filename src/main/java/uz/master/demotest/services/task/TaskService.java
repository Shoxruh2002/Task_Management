package uz.master.demotest.services.task;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.master.demotest.configs.security.UserDetails;
import uz.master.demotest.dto.task.TaskCreateDto;
import uz.master.demotest.dto.task.TaskDto;
import uz.master.demotest.dto.task.TaskUpdateDto;
import uz.master.demotest.entity.action.Action;
import uz.master.demotest.entity.auth.AuthUser;
import uz.master.demotest.entity.column.ProjectColumn;
import uz.master.demotest.entity.task.Task;
import uz.master.demotest.entity.task.Task_Member;
import uz.master.demotest.enums.ActionTexts;
import uz.master.demotest.mappers.TaskMapper;
import uz.master.demotest.repositories.AuthUserRepository;
import uz.master.demotest.repositories.ColumnRepository;
import uz.master.demotest.repositories.TaskRepository;
import uz.master.demotest.services.AbstractService;
import uz.master.demotest.services.GenericCrudService;
import uz.master.demotest.validator.task.TaskValidator;
import uz.master.demotest.validator.Validator;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uz.master.demotest.services.project.ProjectService.FORMATTER;

@Service
public class TaskService extends AbstractService<TaskRepository, TaskMapper, Validator>
        implements GenericCrudService<Task, TaskDto
        , TaskCreateDto, TaskUpdateDto, Long> {


    private final AuthUserRepository authUserRepository;
    private final ColumnRepository columnRepository;

    protected TaskService(TaskRepository repository, TaskMapper mapper, TaskValidator validator, AuthUserRepository authUserRepository, ColumnRepository columnRepository) {
        super(repository, mapper, validator);
        this.authUserRepository = authUserRepository;
        this.columnRepository = columnRepository;
    }

    @Override
    public List<TaskDto> getAll() {
        return mapper.toDto(repository.findAllByDeletedFalse());
    }

    public List<TaskDto> getAll(Long columnId) {
        return repository.findAllByColumnIdAndDeletedFalse(columnId).stream().map(task -> {
            TaskDto taskDto = mapper.toDto(task);
            taskDto.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
            return taskDto;
        }).collect(Collectors.toList());
    }


    @Override
    public Long create(TaskCreateDto createDto) {
        Task task = mapper.fromCreateDto(createDto);
        repository.save(task);
        return task.getId();
    }

    @Override
    public Void delete(Long id) {
        repository.delete(id);
        return null;
    }

    @Override
    public Void update(TaskUpdateDto dto) {
        UserDetails userDetails = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        repository.addAction(dto.getId(), userDetails.getUsername(), ActionTexts.TASK_UPDATED.getText());
        repository.update(dto.getId(), dto.getName(), dto.getDescription(), dto.getTaskOrder(), dto.getLevel(), dto.getPriority());
        return null;
    }

    @Override
    public TaskDto get(Long id) {
        Task task = repository.findByIdAndDeletedFalse(id);
        TaskDto dto = mapper.toDto(task);
        dto.setCreatedAt(task.getCreatedAt().format(DateTimeFormatter.ISO_DATE));
//        dto.setUpdatedAt(task.getUpdatedAt().toString());
        return dto;
    }


    public TaskUpdateDto getUpdateDto(Long id) {
        return mapper.toUpdateDto(this.get(id));
    }

    public Void addMamber(Long taskId, Long memberId) {
        repository.addAction(taskId, authUserRepository.findById(memberId).get().getUsername(), ActionTexts.USER_JOINED.getText());
        repository.addMember(taskId, memberId);
        return null;
    }

    public Void deleteMember(Long taskId, Long memberId) {
        repository.addAction(taskId, authUserRepository.findById(memberId).get().getUsername(), ActionTexts.USER_REMOVED.getText());
        repository.deleteMember(taskId, memberId);
        return null;
    }

    public List<Action> getActions(Long id) {
        return repository.getActions(id);
    }

    public List<AuthUser> getMembers(Long id) {
       return repository.getMembers(id).stream().map(task_member ->
            authUserRepository.findById(task_member.getUserId()).get()
        ).collect(Collectors.toList());
    }

    public void updatePriority(Long id, String code) {

        repository.updatePriority(id, code);
        repository.addAction(id, ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername(), ActionTexts.TASK_PRIORITY_UPDATED.getText());
    }

    public void updateLevel(Long id, String code) {

        repository.updateLevel(id, code);
        repository.addAction(id, ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername(), ActionTexts.TASK_LEVEL_UPDATED.getText());
    }

    public void joinTask(Long id) {
        UserDetails userDetails = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        repository.addAction(id, userDetails.getUsername(), ActionTexts.USER_JOINED.getText());
        repository.addMember(id, userDetails.getId());
    }

    public Long getProjectId(Long id) {
        Task task = repository.findById(id).get();
        return columnRepository.findById(task.getColumnId()).get().getProjectId();
    }

    public void deleteAll(Long id) {
        repository.deleteAllProjectId(id);
    }

    public int getTaskCount(Long id) {
        return repository.getTaskCount(id).size();
    }

    public int getActionCount(String username) {
        return repository.getActionCount(username).size();
    }
}
