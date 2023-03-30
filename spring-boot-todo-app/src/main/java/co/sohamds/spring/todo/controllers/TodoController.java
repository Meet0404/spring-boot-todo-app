package co.sohamds.spring.todo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import co.sohamds.spring.todo.domain.Todo;
import co.sohamds.spring.todo.repository.TodoRepository;

//all the imports for otel
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.*;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.opentelemetry.context.Scope;

@Controller
public class TodoController {
	// Create a Zipkin exporter with the endpoint set to the address of the Zipkin
	// container
	ZipkinSpanExporter exporter = ZipkinSpanExporter.builder().setEndpoint("http://localhost:9411/zipkin/").build();
	// initializes the otel sdk
	OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().build();
	Tracer tracer = openTelemetry.getTracer("To-do-app-Java-Tracer");
	Meter meter = openTelemetry.getMeter("To-do-app-Java-Meter");

	@Autowired
	TodoRepository todoRepository;

	@GetMapping
	public String index() {
		return "index.html";

	}

	@GetMapping("/todos")
	public String todos(Model model) {
		Span span = tracer.spanBuilder("getTodos").startSpan();
		try (Scope scope = span.makeCurrent()) {
			model.addAttribute("todos", todoRepository.findAll());
			span.setAttribute(io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD, "GET");
			span.setAttribute(io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE, "/todos");
		} catch (Throwable t) {
			span.setStatus(StatusCode.ERROR, "An error occurred while processing getTodos request");
		} finally {
			span.end();
		}
		model.addAttribute("todos", todoRepository.findAll());
		return "todos";
	}

	@PostMapping("/todoNew")
	public String add(@RequestParam String todoItem, @RequestParam String status, Model model) {

		Span span = tracer.spanBuilder("addTodo").startSpan();
		Todo todo = new Todo(todoItem, status);
		todo.setTodoItem(todoItem);
		todo.setCompleted(status);
		todoRepository.save(todo);
		model.addAttribute("todos", todoRepository.findAll());
		span.setAttribute(SemanticAttributes.HTTP_METHOD, "POST");
		span.setAttribute(SemanticAttributes.HTTP_ROUTE, "/todoNew");
		span.end();
		return "redirect:/todos";
	}

	@PostMapping("/todoDelete/{id}")
	public String delete(@PathVariable long id, Model model) {
		todoRepository.deleteById(id);
		model.addAttribute("todos", todoRepository.findAll());
		return "redirect:/todos";
	}

	@PostMapping("/todoUpdate/{id}")
	public String update(@PathVariable long id, Model model) {
		Todo todo = todoRepository.findById(id).get();
		if ("Yes".equals(todo.getCompleted())) {
			todo.setCompleted("No");
		} else {
			todo.setCompleted("Yes");
		}
		todoRepository.save(todo);
		model.addAttribute("todos", todoRepository.findAll());
		return "redirect:/todos";
	}
}