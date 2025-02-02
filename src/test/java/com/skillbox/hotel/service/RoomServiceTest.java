package com.skillbox.hotel.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skillbox.hotel.model.Room;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

class RoomServiceTest {

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
    }

    @Test
    void addRoom_ShouldAddRoomToList() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);

        // Act
        roomService.addRoom(room);

        // Assert
        assertThat(roomService.getAllRooms())
                .hasSize(1)
                .containsExactly(room);
    }

    @Test
    void getAvailableRooms_ShouldFilterCorrectly() {
        // Arrange
        roomService.addRoom(new Room(1L, "Standard", 100.0, true));
        roomService.addRoom(new Room(2L, "Deluxe", 200.0, false));
        roomService.addRoom(new Room(3L, "Suite", 300.0, true));

        Predicate<Room> availableAndPriceFilter = room ->
                room.isAvailable() && room.getPrice() > 150.0;

        // Act
        List<Room> result = roomService.getAvailableRooms(availableAndPriceFilter);

        // Assert
        assertThat(result)
                .hasSize(1)
                .extracting(Room::getRoomId)
                .containsExactly(3L);
    }

    @Test
    void findRoomById_ShouldReturnCorrectRoom() {
        // Arrange
        Room expected = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(expected);
        roomService.addRoom(new Room(2L, "Deluxe", 200.0, false));

        // Act
        Optional<Room> result = roomService.findRoomById(1L);

        // Assert
        assertThat(result)
                .isPresent()
                .containsSame(expected);
    }

    @Test
    void findRoomById_ShouldReturnEmptyForNonExistingId() {
        // Act
        Optional<Room> result = roomService.findRoomById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void updateRoomAvailability_ShouldChangeRoomStatus() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        roomService.updateRoomAvailability(1L, false);

        // Assert
        assertThat(room.isAvailable()).isFalse();
    }

    @Test
    void updateRoomAvailability_ShouldDoNothingForWrongId() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        roomService.updateRoomAvailability(999L, false);

        // Assert
        assertThat(room.isAvailable()).isTrue();
    }

    @Test
    void getAllRooms_ShouldReturnUnmodifiableCollection() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        List<Room> result = roomService.getAllRooms();

        // Assert
        // Проверяем, что попытка модификации вызывает исключение
        Assertions.assertThatThrownBy(() -> result.add(new Room(2L, "Deluxe", 200.0, false)))
                .isInstanceOf(UnsupportedOperationException.class);

        // Дополнительная проверка, что исходная коллекция не изменилась
        Assertions.assertThat(roomService.getAllRooms())
                .hasSize(1)
                .extracting(Room::getRoomId)
                .containsExactly(1L);
    }

    @Test
    void shouldHandleNullValuesSafely() {
        // Act & Assert
        Assertions.assertThatCode(() -> {
            roomService.addRoom(null);
            roomService.findRoomById(null);
            roomService.updateRoomAvailability(null, true);
        }).doesNotThrowAnyException();

        assertThat(roomService.getAvailableRooms(null)).isEmpty();
    }

    @Test
    void shouldHandleDuplicateRoomIds() {
        // Arrange
        Room room1 = new Room(1L, "Standard", 100.0, true);
        Room room2 = new Room(1L, "Deluxe", 200.0, false);

        // Act
        roomService.addRoom(room1);
        roomService.addRoom(room2);

        // Assert
        assertThat(roomService.getAllRooms())
                .hasSize(2)
                .extracting(Room::getRoomId)
                .containsExactly(1L, 1L);
    }

    @Test
    void shouldHandleEmptyRoomList() {
        // Act & Assert
        assertThat(roomService.getAllRooms()).isEmpty();
        assertThat(roomService.getAvailableRooms(r -> true)).isEmpty();
        assertThat(roomService.findRoomById(1L)).isEmpty();
    }
}
