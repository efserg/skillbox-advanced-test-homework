package com.skillbox.hotel.service;


import com.skillbox.hotel.model.Booking;
import com.skillbox.hotel.model.Room;
import com.skillbox.hotel.service.external.NotificationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BookingServiceTest {

    @Mock
    private RoomService roomService;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Long> customerIdCaptor;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(roomService, notificationService);
    }

    @Test
    void createBooking_ShouldSuccess_WhenRoomAvailable() {
        // Arrange
        Long roomId = 101L;
        Long bookingId = 1L;
        Long customerId = 123L;
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(3);
        Room mockRoom = new Room(roomId, "Standard", 100.0, true);

        when(roomService.findRoomById(roomId)).thenReturn(Optional.of(mockRoom));

        // Act
        Booking result = bookingService.createBooking(bookingId, roomId, customerId, startDate, endDate);

        // Assert
        Assertions.assertThat(result)
                .isNotNull()
                .extracting(
                        Booking::getBookingId,
                        Booking::getRoomId,
                        Booking::getCustomerId,
                        Booking::getStartDate,
                        Booking::getEndDate
                )
                .containsExactly(bookingId, roomId, customerId, startDate, endDate);

        verify(roomService).updateRoomAvailability(roomId, false);
        verify(notificationService).sendNotification(customerIdCaptor.capture(), messageCaptor.capture());

        Assertions.assertThat(customerIdCaptor.getValue()).isEqualTo(customerId);
        Assertions.assertThat(messageCaptor.getValue()).contains("подтверждено");
    }

    @Test
    void createBooking_ShouldThrow_WhenRoomNotAvailable() {
        // Arrange
        Long roomId = 102L;
        Room mockRoom = new Room(roomId, "Deluxe", 200.0, false);
        when(roomService.findRoomById(roomId)).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        Assertions.assertThatThrownBy(() ->
                        bookingService.createBooking(1L, roomId, 123L, LocalDate.now(), LocalDate.now().plusDays(1))
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("недоступен");

        verify(roomService, never()).updateRoomAvailability(anyLong(), any());
        verify(notificationService, never()).sendNotification(anyLong(), any());
    }

    @Test
    void createBooking_ShouldThrow_WhenRoomNotFound() {
        // Arrange
        when(roomService.findRoomById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() ->
                        bookingService.createBooking(1L, 999L, 123L, LocalDate.now(), LocalDate.now().plusDays(1))
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("недоступен");
    }

    @Test
    void createBooking_ShouldThrow_WhenInvalidDates() {
        // Arrange
        Long roomId = 103L;
        Room mockRoom = new Room(roomId, "Suite", 300.0, true);
        when(roomService.findRoomById(roomId)).thenReturn(Optional.of(mockRoom));

        // Act & Assert
        Assertions.assertThatThrownBy(() ->
                        bookingService.createBooking(1L, roomId, 123L, LocalDate.now().plusDays(2), LocalDate.now())
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Дата начала бронирования должна быть раньше даты окончания");
    }

    @Test
    void cancelBooking_ShouldSuccess_WhenBookingExists() {
        // Arrange
        Long bookingId = 1L;
        Long roomId = 101L;
        Long customerId = 123L;
        Booking existingBooking = new Booking(bookingId, roomId, customerId, LocalDate.now(), LocalDate.now().plusDays(1));
        bookingService.createBooking(bookingId, roomId, customerId, existingBooking.getStartDate(), existingBooking.getEndDate());

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        verify(roomService).updateRoomAvailability(roomId, true);
        verify(notificationService).sendNotification(customerIdCaptor.capture(), messageCaptor.capture());

        Assertions.assertThat(customerIdCaptor.getValue()).isEqualTo(customerId);
        Assertions.assertThat(messageCaptor.getValue()).contains("отменено");
        Assertions.assertThat(bookingService.getAllBookings()).isEmpty();
    }

    @Test
    void cancelBooking_ShouldThrow_WhenBookingNotFound() {
        // Act & Assert
        Assertions.assertThatThrownBy(() -> bookingService.cancelBooking(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не найдено");
    }

    @Test
    void createBooking_ShouldHandleNullParameters() {
        // Act & Assert
        Assertions.assertThatThrownBy(() ->
                        bookingService.createBooking(null, null, null, null, null)
                )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("недопустимые параметры");
    }

    // Вспомогательный метод для проверки состояния бронирований
    private List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }
}