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
        if (event == null) throw new IllegalArgumentException("Event cannot be null");
        if (event.getId() == null) event.setId(UUID.randomUUID());
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
        if (!eventRepository.existsById(id)) throw new IllegalArgumentException("Event not found");
        event.setId(id);
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) throw new IllegalArgumentException("Event not found");
        eventRepository.deleteById(id);
    }

    @Override
    public Event partialUpdateEvent(UUID id, Event partialEvent) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (partialEvent.getEventName() != null) existing.setEventName(partialEvent.getEventName());
        if (partialEvent.getTags() != null && !partialEvent.getTags().isEmpty()) existing.setTags(partialEvent.getTags());
        if (partialEvent.getTicketPrice() != null && partialEvent.getTicketPrice().compareTo(BigDecimal.ZERO) >= 0)
            existing.setTicketPrice(partialEvent.getTicketPrice());
        if (partialEvent.getEventDateTime() != null) existing.setEventDateTime(partialEvent.getEventDateTime());
        if (partialEvent.getDurationMinutes() > 0) existing.setDurationMinutes(partialEvent.getDurationMinutes());

        return eventRepository.save(existing);
    }

    @Override
    public List<Event> getEventsByTag(String tag) {
        if (tag == null || tag.isBlank()) return List.of();
        String searchTag = tag.trim().toLowerCase();
        return eventRepository.findAll().stream()
                .filter(event -> event.getTags() != null &&
                        event.getTags().stream().anyMatch(t -> t != null && t.toLowerCase().contains(searchTag)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findAll().stream()
                .filter(event -> event.getEventDateTime() != null && event.getEventDateTime().isAfter(now))
                .sorted(Comparator.comparing(Event::getEventDateTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal min = (minPrice == null) ? BigDecimal.ZERO : minPrice;
        BigDecimal max = (maxPrice == null) ? new BigDecimal("1000000") : maxPrice;
        if (min.compareTo(max) > 0) { BigDecimal temp = min; min = max; max = temp; }

        BigDecimal finalMin = min, finalMax = max;
        return eventRepository.findAll().stream()
                .filter(e -> e.getTicketPrice() != null &&
                        e.getTicketPrice().compareTo(finalMin) >= 0 &&
                        e.getTicketPrice().compareTo(finalMax) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return List.of();
        LocalDateTime s = start.isBefore(end) ? start : end;
        LocalDateTime e = start.isBefore(end) ? end : start;

        LocalDateTime finalS = s, finalE = e;
        return eventRepository.findAll().stream()
                .filter(event -> event.getEventDateTime() != null &&
                        !event.getEventDateTime().isBefore(finalS) &&
                        !event.getEventDateTime().isAfter(finalE))
                .collect(Collectors.toList());
    }

    @Override
    public Event updateEventPrice(UUID id, BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Invalid price");

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        event.setTicketPrice(newPrice);
        return eventRepository.save(event);
    }
}