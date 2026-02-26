package az.edu.ada.wm2.lab5.service;

import az.edu.ada.wm2.lab5.model.Event;
import az.edu.ada.wm2.lab5.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Event createEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }

        return eventRepository.save(event);
    }

    @Override
    public Event getEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));
    }

    @Override
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Override
    public Event updateEvent(UUID id, Event event) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        event.setId(id);
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }

        eventRepository.deleteById(id);
    }

    @Override
    public Event partialUpdateEvent(UUID id, Event partialEvent) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));

        if (partialEvent == null) {
            return existingEvent;
        }

        if (partialEvent.getEventName() != null) {
            existingEvent.setEventName(partialEvent.getEventName());
        }

        if (partialEvent.getTags() != null && !partialEvent.getTags().isEmpty()) {
            existingEvent.setTags(partialEvent.getTags());
        }

        if (partialEvent.getTicketPrice() != null) {
            if (partialEvent.getTicketPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ticket price cannot be negative");
            }
            existingEvent.setTicketPrice(partialEvent.getTicketPrice());
        }

        if (partialEvent.getEventDateTime() != null) {
            existingEvent.setEventDateTime(partialEvent.getEventDateTime());
        }

        if (partialEvent.getDurationMinutes() > 0) {
            existingEvent.setDurationMinutes(partialEvent.getDurationMinutes());
        }

        return eventRepository.save(existingEvent);
    }

    // -------- Custom Methods --------

    @Override
    public List<Event> getEventsByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }

        String searchTag = tag.trim().toLowerCase();

        return eventRepository.findAll().stream()
                .filter(event -> event.getTags() != null &&
                        event.getTags().stream()
                                .anyMatch(t -> t != null && t.toLowerCase().contains(searchTag)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();

        return eventRepository.findAll().stream()
                .filter(event -> event.getEventDateTime() != null &&
                        event.getEventDateTime().isAfter(now))
                .sorted(Comparator.comparing(Event::getEventDateTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {

        BigDecimal effectiveMin = (minPrice == null) ? BigDecimal.ZERO : minPrice;
        BigDecimal effectiveMax = (maxPrice == null) ? new BigDecimal("1000000") : maxPrice;

        if (effectiveMin.compareTo(effectiveMax) > 0) {
            BigDecimal temp = effectiveMin;
            effectiveMin = effectiveMax;
            effectiveMax = temp;
        }

        BigDecimal finalMin = effectiveMin;
        BigDecimal finalMax = effectiveMax;

        return eventRepository.findAll().stream()
                .filter(event -> event.getTicketPrice() != null &&
                        event.getTicketPrice().compareTo(finalMin) >= 0 &&
                        event.getTicketPrice().compareTo(finalMax) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {

        if (start == null || end == null) {
            return List.of();
        }

        LocalDateTime effectiveStart = start;
        LocalDateTime effectiveEnd = end;

        if (start.isAfter(end)) {
            effectiveStart = end;
            effectiveEnd = start;
        }

        LocalDateTime finalStart = effectiveStart;
        LocalDateTime finalEnd = effectiveEnd;

        return eventRepository.findAll().stream()
                .filter(event -> event.getEventDateTime() != null &&
                        !event.getEventDateTime().isBefore(finalStart) &&
                        !event.getEventDateTime().isAfter(finalEnd))
                .collect(Collectors.toList());
    }

    @Override
    public Event updateEventPrice(UUID id, BigDecimal newPrice) {

        if (newPrice == null) {
            throw new IllegalArgumentException("New price cannot be null");
        }

        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ticket price cannot be negative");
        }

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + id));

        event.setTicketPrice(newPrice);
        return eventRepository.save(event);
    }
}