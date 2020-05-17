package hu.bme.sch.sssl.doktor.util

import hu.bme.sch.sssl.doktor.`enum`.MessageStatus
import hu.bme.sch.sssl.doktor.repository.MessageRepository.MessageDbo
import hu.bme.sch.sssl.doktor.service.NewMessageService.CreateMessageDto

object MessageTransformerUtil {
  implicit class CreateMessageDtoTransformer(dto: CreateMessageDto) {
    def toMessageDbo(
        implicit
        up: UuidProvider,
        tp: TimeProvider,
    ): MessageDbo = {
      val isShown = dto.status == MessageStatus.Shown
      MessageDbo(
        up.generateUuid,
        dto.ticketId,
        dto.uid,
        dto.createdBy,
        tp.epochMillis,
        dto.status,
        dto.text,
        Some(dto.uid).filter(_ => isShown),
        Some(dto.createdBy).filter(_ => isShown),
        Some(tp.epochMillis).filter(_ => isShown),
      )
    }
  }
}
