package co.sohamds.spring.todo.controllers;

import java.util.logging.*;
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
import io.opentelemetry.api.*;
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
	ZipkinSpanExporter exporter = ZipkinSpanExporter.builder().setEndpoint("http://localhost:9411/zipkin/")
			.build();
	// initializes the otel sdk
	static OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().build();
	private static final Tracer tracer = openTelemetry.getTracer("To-do-app-Java-Tracer");
	Meter meter = openTelemetry.getMeter("To-do-app-Java-Meter");
	Logger logger = Logger.getLogger(TodoController.class.getName());

	// const collectorOptions={
	// serviceName: '<Zipkin>',
	// url: 'http://localhost:9411/zipkin/',
	// headers
	// };

	// const exporter=new CollectorTraceExporter(collectorOptions);
	@Autowired
	TodoRepository todoRepository;

	@GetMapping
	public String index() {
		return "index.html";

	}

	@GetMapping("/todos")
	public String todos(Model model) {
		Span span = tracer.spanBuilder("getTodos").startSpan();

		logger.setLevel(Level.FINE);
		model.addAttribute("todos", todoRepository.findAll());
		span.setAttribute("Adding a todo to the list", "/todos");
		span.addEvent("in todos model");
		logger.log(Level.FINE, "in the todos which accepts model as an attribute");
		span.end();
		span.setStatus(StatusCode.ERROR, "An error occurred while processing getTodos request");
		model.addAttribute("todos", todoRepository.findAll());
		return "todos";
	}

	@PostMapping("/todoNew")
	public String add(@RequestParam String todoItem, @RequestParam String status, Model model) {

		// logger
		logger.setLevel(Level.FINE);
		logger.log(Level.FINE,
				"in the add method of the controller which gets item to be inserted, status, and model as an attribute");
		// creating a span to instrument for adding a todo
		Span span = tracer.spanBuilder("addTodo").startSpan();
		Todo todo = new Todo(todoItem, status);
		todo.setTodoItem(todoItem);
		todo.setCompleted(status);
		todoRepository.save(todo);
		model.addAttribute("todos", todoRepository.findAll());
		// instrumented code
		span.setAttribute("Adding a todo to the list", todoItem);
		span.addEvent("Todo Added");
		span.end();
		return "redirect:/todos";
	}

	@PostMapping("/todoDelete/{id}")
	public String delete(@PathVariable long id, Model model) {
		// logger
		logger.setLevel(Level.FINE);
		logger.log(Level.FINE,
				"in the delete method of the controller which gets item's id to be deleted, and model as an attribute");
		// creating a span to instrument for deleting a todo
		Span span = tracer.spanBuilder("deleteTodo").startSpan();
		span.addEvent("Delete a To do");
		span.setAttribute("Deleting todo with", id);
		// instrumented code ends
		todoRepository.deleteById(id);
		model.addAttribute("todos", todoRepository.findAll());
		span.end();
		return "redirect:/todos";
	}

	@PostMapping("/todoUpdate/{id}")
	public String update(@PathVariable long id, Model model) {

		// logger
		logger.setLevel(Level.FINE);
		logger.log(Level.FINE,
				"in the update method of the controller which gets item's id to be update, and model as an attribute");
		// creating a span to instrument for Updating a todo
		Span span = tracer.spanBuilder("Updating a To-do").startSpan();
		Todo todo = todoRepository.findById(id).get();
		if ("Yes".equals(todo.getCompleted())) {
			span.setAttribute("Todo not completed", null);
			todo.setCompleted("No");
		} else {
			span.setAttribute("Todo completed", null);
			todo.setCompleted("Yes");
		}
		todoRepository.save(todo);
		span.end();

		model.addAttribute("todos", todoRepository.findAll());
		return "redirect:/todos";
	}
}