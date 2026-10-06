package com.secureops.controller;

import com.secureops.dto.Dtos.ActionResult;
import com.secureops.dto.Dtos.TargetDto;
import com.secureops.dto.Dtos.TargetRequest;
import com.secureops.service.TargetService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/targets")
public class TargetController {
    private final TargetService targets;

    public TargetController(TargetService targets) { this.targets = targets; }

    @GetMapping public List<TargetDto> list() { return targets.list(); }

    @GetMapping("/{id}") public TargetDto get(@PathVariable int id) { return targets.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TargetDto create(@RequestBody TargetRequest body) { return targets.create(body); }

    @PutMapping("/{id}") public TargetDto update(@PathVariable int id, @RequestBody TargetRequest body) { return targets.update(id, body); }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) { targets.delete(id); }

    @PostMapping("/{id}/test") public ActionResult test(@PathVariable int id) { return targets.test(id); }

    /** Runs one collection immediately (same code path as the scheduler). */
    @PostMapping("/{id}/collect") public ActionResult collect(@PathVariable int id) { return targets.collectNow(id); }
}
